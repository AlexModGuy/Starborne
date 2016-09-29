package net.starborne.client;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.starborne.Starborne;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.world.StructureWorld;
import net.starborne.server.message.BreakBlockEntityMessage;
import net.starborne.server.message.InteractBlockEntityMessage;

import java.util.HashMap;
import java.util.Map;

public class ClientEventHandler {
    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();

    public static Map.Entry<StructureEntity, RayTraceResult> mouseOver;

    private int interactCooldown;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        World world = MINECRAFT.theWorld;
        EntityPlayerSP player = MINECRAFT.thePlayer;
        if (player != null) {
            mouseOver = this.getSelectedBlock(player);
        }
        if (this.interactCooldown > 0) {
            this.interactCooldown--;
        }
        if (player != null && mouseOver != null && MINECRAFT.gameSettings.keyBindPickBlock.isKeyDown()) {
            ItemStack result;
            StructureEntity structure = mouseOver.getKey();
            TileEntity tile = null;
            RayTraceResult target = mouseOver.getValue();
            if (target.typeOfHit == RayTraceResult.Type.BLOCK) {
                StructureWorld structureWorld = structure.structureWorld;
                IBlockState state = structureWorld.getBlockState(target.getBlockPos());
                if (!state.getBlock().isAir(state, structureWorld, target.getBlockPos())) {
                    if (player.capabilities.isCreativeMode && GuiScreen.isCtrlKeyDown() && state.getBlock().hasTileEntity(state)) {
                        tile = structureWorld.getTileEntity(target.getBlockPos());
                    }
                    result = state.getBlock().getPickBlock(state, target, structureWorld, target.getBlockPos(), player);
                    if (tile != null) {
                        MINECRAFT.storeTEInStack(result, tile);
                    }
                    if (player.capabilities.isCreativeMode) {
                        player.inventory.setPickedItemStack(result);
                        MINECRAFT.playerController.sendSlotPacket(player.getHeldItem(EnumHand.MAIN_HAND), player.inventory.currentItem + 36);
                    } else {
                        int slot = player.inventory.getSlotFor(result);
                        if (slot != -1) {
                            if (InventoryPlayer.isHotbar(slot)) {
                                player.inventory.currentItem = slot;
                            } else {
                                MINECRAFT.playerController.pickItem(slot);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickAir(PlayerInteractEvent.RightClickEmpty event) {
        this.interactStructure(event.getEntityPlayer(), event.getHand());
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        EnumHand hand = event.getHand();
        if (!this.interactStructure(player, hand)) {
            event.setCanceled(true);
            ItemStack heldItem = player.getHeldItem(hand);
            if (heldItem != null) {
                int prevSize = heldItem.stackSize;
                ActionResult<ItemStack> result = heldItem.useItemRightClick(player.worldObj, player, hand);
                if (result.getType() != EnumActionResult.SUCCESS) {
                    for (Entity entity : player.worldObj.loadedEntityList) {
                        if (entity instanceof StructureEntity) {
                            StructureEntity structure = (StructureEntity) entity;
                            result = heldItem.useItemRightClick(structure.structureWorld, player, hand);
                            if (result.getType() == EnumActionResult.SUCCESS) {
                                break;
                            }
                        }
                    }
                }
                ItemStack output = result.getResult();
                if (output != heldItem || output.stackSize != prevSize) {
                    player.setHeldItem(hand, output);
                    if (output.stackSize <= 0) {
                        player.setHeldItem(hand, null);
                        ForgeEventFactory.onPlayerDestroyItem(player, output, hand);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onClickAir(PlayerInteractEvent.LeftClickEmpty event) {
        EntityPlayer player = event.getEntityPlayer();
        EnumHand hand = event.getHand();
        if (mouseOver != null && this.interactCooldown <= 0) {
            this.interactCooldown = 3;
            StructureEntity structure = mouseOver.getKey();
            RayTraceResult result = mouseOver.getValue();
            BlockPos pos = result.getBlockPos();
            IBlockState state = structure.getBlockState(pos);
            if (player.capabilities.isCreativeMode) {
                structure.structureWorld.playEvent(2001, pos, Block.getStateId(state));
                structure.setBlockState(pos, Blocks.AIR.getDefaultState());
                Starborne.networkWrapper.sendToServer(new BreakBlockEntityMessage(structure.getEntityId(), pos));
            }
        }
    }

    private boolean interactStructure(EntityPlayer player, EnumHand hand) {
        if (mouseOver != null && this.interactCooldown <= 0) {
            this.interactCooldown = 3;
            StructureEntity structure = mouseOver.getKey();
            RayTraceResult result = mouseOver.getValue();
            BlockPos pos = result.getBlockPos();
            Vec3d hitVec = result.hitVec;
            IBlockState state = structure.getBlockState(pos);
            float hitX = (float) (hitVec.xCoord - pos.getX());
            float hitY = (float) (hitVec.yCoord - pos.getY());
            float hitZ = (float) (hitVec.zCoord - pos.getZ());
            ItemStack heldItem = player.getHeldItem(hand);
            Starborne.networkWrapper.sendToServer(new InteractBlockEntityMessage(structure.getEntityId(), pos, result.sideHit, hitX, hitY, hitZ, hand));
            if (state.getBlock().onBlockActivated(structure.structureWorld, pos, state, player, hand, heldItem, result.sideHit, hitX, hitY, hitZ)) {
                player.swingArm(hand);
                return true;
            } else if (heldItem != null) {
                int size = heldItem.stackSize;
                EnumActionResult actionResult = heldItem.onItemUse(player, structure.structureWorld, pos, hand, result.sideHit, hitX, hitY, hitZ);
                if (actionResult == EnumActionResult.SUCCESS) {
                    player.swingArm(hand);
                }
                if (player.capabilities.isCreativeMode && heldItem.stackSize < size) {
                    heldItem.stackSize = size;
                }
                return true;
            }
        }
        return false;
    }

    private Map.Entry<StructureEntity, RayTraceResult> getSelectedBlock(EntityPlayer player) {
        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        double x = player.posX;
        double y = player.posY + player.getEyeHeight();
        double z = player.posZ;
        Vec3d start = new Vec3d(x, y, z);
        float pitchHorizontalFactor = -MathHelper.cos(-pitch * 0.017453292F);
        float deltaY = MathHelper.sin(-pitch * 0.017453292F);
        float deltaX = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI) * pitchHorizontalFactor;
        float deltaZ = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI) * pitchHorizontalFactor;
        double reach = 5.0;
        if (player instanceof EntityPlayerMP) {
            reach = ((EntityPlayerMP) player).interactionManager.getBlockReachDistance();
        }
        Vec3d end = start.addVector(deltaX * reach, deltaY * reach, deltaZ * reach);
        Map<StructureEntity, RayTraceResult> results = new HashMap<>();
        for (Entity entity : player.worldObj.loadedEntityList) {
            if (entity instanceof StructureEntity) {
                StructureEntity structure = (StructureEntity) entity;
                RayTraceResult result = structure.structureWorld.rayTraceBlocks(start, end);
                if (result != null && result.typeOfHit != RayTraceResult.Type.MISS) {
                    results.put(structure, result);
                }
            }
        }
        if (results.size() > 0) {
            Map.Entry<StructureEntity, RayTraceResult> closest = null;
            double closestDistance = Double.MAX_VALUE;
            for (Map.Entry<StructureEntity, RayTraceResult> entry : results.entrySet()) {
                RayTraceResult result = entry.getValue();
                double distance = result.hitVec.distanceTo(entry.getKey().getTransformedPosition(start));
                if (distance < closestDistance) {
                    closest = entry;
                    closestDistance = distance;
                }
            }
            return closest;
        }
        return null;
    }
}
