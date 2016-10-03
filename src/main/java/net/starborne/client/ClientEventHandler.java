package net.starborne.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.starborne.Starborne;
import net.starborne.client.render.entity.structure.StructureEntityRenderer;
import net.starborne.server.ServerStructureHandler;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.StructurePlayerHandler;

public class ClientEventHandler {
    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        WorldClient world = MINECRAFT.theWorld;
        if (world != null) {
            Starborne.PROXY.getStructureHandler(world).update(world);
        }
    }

    @SubscribeEvent
    public void onRightClickAir(PlayerInteractEvent.RightClickEmpty event) {
        EntityPlayer player = event.getEntityPlayer();
        ServerStructureHandler structureHandler = Starborne.PROXY.getStructureHandler(event.getWorld());
        structureHandler.interact(structureHandler.get(structureHandler.getMousedOver(player), player), player, event.getHand());
    }

    @SubscribeEvent
    public void onClickAir(PlayerInteractEvent.LeftClickEmpty event) {
        EntityPlayer player = event.getEntityPlayer();
        StructureEntity mousedOver = Starborne.PROXY.getStructureHandler(event.getWorld()).getMousedOver(player);
        if (mousedOver != null) {
            StructurePlayerHandler mouseOverHandler = Starborne.PROXY.getStructureHandler(event.getWorld()).get(mousedOver, player);
            if (mouseOverHandler != null) {
                RayTraceResult mouseOver = mouseOverHandler.getMouseOver();
                if (mouseOver != null && mouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
                    mouseOverHandler.clickBlock(mouseOver.getBlockPos());
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof StructureEntity) {
            Starborne.PROXY.getStructureHandler(event.getWorld()).addEntity((StructureEntity) entity);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        Starborne.PROXY.getStructureHandler(event.getWorld()).clearEntities();
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        EntityPlayer player = MINECRAFT.thePlayer;
        float partialTicks = event.getPartialTicks();
        double playerX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double playerY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double playerZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
        ServerStructureHandler structureHandler = Starborne.PROXY.getStructureHandler(MINECRAFT.theWorld);
        GlStateManager.depthMask(true);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.disableBlend();
        for (StructureEntity entity : structureHandler.getStructures()) {
            double entityX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
            double entityY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
            double entityZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
            float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
            float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
            float roll = entity.prevRotationRoll + (entity.rotationRoll - entity.prevRotationRoll) * partialTicks;
            StructureEntityRenderer.renderWorld(entity, entityX - playerX, entityY - playerY, entityZ - playerZ, yaw, pitch, roll, partialTicks);
        }
    }
}
