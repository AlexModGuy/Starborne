package net.starborne.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.starborne.Starborne;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.message.InteractBlockEntityMessage;

import java.util.HashMap;
import java.util.Map;

public class ClientEventHandler {
    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();

    public static Map.Entry<StructureEntity, RayTraceResult> mouseOver;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (MINECRAFT.thePlayer != null) {
            mouseOver = this.getSelectedBlock(MINECRAFT.thePlayer);
        }
    }

    @SubscribeEvent
    public void onRightClickAir(PlayerInteractEvent.RightClickEmpty event) {
        this.interactStructure(event.getEntityPlayer(), event.getHand());
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        this.interactStructure(event.getEntityPlayer(), event.getHand());
    }

    private void interactStructure(EntityPlayer player, EnumHand hand) {
        if (mouseOver != null) {
            StructureEntity structure = mouseOver.getKey();
            RayTraceResult result = mouseOver.getValue();
            BlockPos pos = result.getBlockPos();
            Vec3d hitVec = result.hitVec;
            IBlockState state = structure.getBlockState(pos);
            float hitX = (float) (hitVec.xCoord - pos.getX());
            float hitY = (float) (hitVec.yCoord - pos.getY());
            float hitZ = (float) (hitVec.zCoord - pos.getZ());
            if (state.getBlock().onBlockActivated(structure.structureWorld, pos, state, player, hand, player.getHeldItem(hand), result.sideHit, hitX, hitY, hitZ)) {
                player.swingArm(hand);
            }
            Starborne.networkWrapper.sendToServer(new InteractBlockEntityMessage(structure.getEntityId(), pos, result.sideHit, hitX, hitY, hitZ, hand));
        }
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
