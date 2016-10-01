package net.starborne.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.StructurePlayerHandler;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class ClientEventHandler {
    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();

    public static Map<StructureEntity, StructurePlayerHandler> handlers = new HashMap<>();
    public static StructureEntity mousedOver;

    private static ArrayDeque<StructureEntity> queuedHandlers = new ArrayDeque<>();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        EntityPlayerSP player = MINECRAFT.thePlayer;
        if (player != null) {
            if (ClientEventHandler.queuedHandlers.size() > 0) {
                StructureEntity structureEntity = ClientEventHandler.queuedHandlers.poll();
                ClientEventHandler.handlers.put(structureEntity, new StructurePlayerHandler(structureEntity, MINECRAFT.thePlayer));
            }

            Map.Entry<StructureEntity, RayTraceResult> mouseOver = this.getSelectedBlock(player);

            StructureEntity currentMousedOver = mouseOver != null ? mouseOver.getKey() : null;

            if (mouseOver != null) {
                StructurePlayerHandler mouseOverHandler = ClientEventHandler.handlers.get(mouseOver.getKey());
                if (mouseOverHandler != null) {
                    RayTraceResult prevMouseOver = mouseOverHandler.getMouseOver();
                    mouseOverHandler.setMouseOver(mouseOver.getValue());
                    if (prevMouseOver == null || !prevMouseOver.getBlockPos().equals(mouseOver.getValue().getBlockPos())) {
                        mouseOverHandler.startBreaking(null);
                    }
                }
            }

            if (mousedOver != null && mousedOver != currentMousedOver) {
                StructurePlayerHandler mouseOverHandler = ClientEventHandler.handlers.get(mousedOver);
                if (mouseOverHandler != null) {
                    mouseOverHandler.startBreaking(null);
                    mouseOverHandler.setMouseOver(null);
                }
            }

            mousedOver = currentMousedOver;

            if (mousedOver != null && !MINECRAFT.gameSettings.keyBindAttack.isKeyDown()) {
                if (mousedOver != null) {
                    StructurePlayerHandler mouseOverHandler = ClientEventHandler.handlers.get(mousedOver);
                    if (mouseOverHandler != null) {
                        mouseOverHandler.startBreaking(null);
                    }
                }
            }

            for (Map.Entry<StructureEntity, StructurePlayerHandler> entry : ClientEventHandler.handlers.entrySet()) {
                entry.getValue().update();
            }

            if (mousedOver != null && MINECRAFT.gameSettings.keyBindPickBlock.isKeyDown()) {
                StructurePlayerHandler handler = ClientEventHandler.handlers.get(mousedOver);
                if (handler != null) {
                    handler.onPickBlock();
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickAir(PlayerInteractEvent.RightClickEmpty event) {
        this.interactStructure(event.getHand());
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        EnumHand hand = event.getHand();
        if (!this.interactStructure(hand)) {
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
                    if (player.capabilities.isCreativeMode && output.stackSize < prevSize) {
                        output.stackSize = prevSize;
                    }
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
        if (mousedOver != null) {
            StructurePlayerHandler mouseOverHandler = ClientEventHandler.handlers.get(mousedOver);
            if (mouseOverHandler != null) {
                RayTraceResult mouseOver = mouseOverHandler.getMouseOver();
                if (mouseOver != null && mouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
                    mouseOverHandler.clickBlock(mouseOver.getBlockPos());
                }
            }
        }
    }

    private boolean interactStructure(EnumHand hand) {
        if (mousedOver != null) {
            StructurePlayerHandler mouseOverHandler = ClientEventHandler.handlers.get(mousedOver);
            if (mouseOverHandler != null) {
                RayTraceResult mouseOver = mouseOverHandler.getMouseOver();
                if (mouseOver != null && mouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
                    return mouseOverHandler.interact(hand);
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            ClientEventHandler.queuedHandlers.add(structureEntity);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        ClientEventHandler.handlers.clear();
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
