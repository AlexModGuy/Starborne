package net.starborne.client.render.blocksystem;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.starborne.server.blocksystem.BlockSystem;

import java.util.HashMap;
import java.util.Map;

public class BlockSystemRenderHandler {
    private static final Map<BlockSystem, BlockSystemRenderer> RENDERERS = new HashMap<>();

    public static void addBlockSystem(BlockSystem blockSystem) {
        RENDERERS.put(blockSystem, new BlockSystemRenderer(blockSystem));
    }

    public static void removeBlockSystem(BlockSystem blockSystem) {
        RENDERERS.remove(blockSystem);
    }

    public static BlockSystemRenderer get(BlockSystem blockSystem) {
        return RENDERERS.get(blockSystem);
    }

    public static void render(EntityPlayer player, double playerX, double playerY, double playerZ, float partialTicks) {
        GlStateManager.depthMask(true);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.disableBlend();
        for (Map.Entry<BlockSystem, BlockSystemRenderer> entry : RENDERERS.entrySet()) {
            BlockSystem blockSystem = entry.getKey();
            BlockSystemRenderer renderer = entry.getValue();
            double entityX = blockSystem.prevPosX + (blockSystem.posX - blockSystem.prevPosX) * partialTicks;
            double entityY = blockSystem.prevPosY + (blockSystem.posY - blockSystem.prevPosY) * partialTicks;
            double entityZ = blockSystem.prevPosZ + (blockSystem.posZ - blockSystem.prevPosZ) * partialTicks;
            float rotationX = blockSystem.prevRotationX + (blockSystem.rotationX - blockSystem.prevRotationX) * partialTicks;
            float rotationY = blockSystem.prevRotationY + (blockSystem.rotationY - blockSystem.prevRotationY) * partialTicks;
            float rotationZ = blockSystem.prevRotationZ + (blockSystem.rotationZ - blockSystem.prevRotationZ) * partialTicks;
            renderer.renderBlockSystem(player, entityX - playerX, entityY - playerY, entityZ - playerZ, rotationX, rotationY, rotationZ, partialTicks);
        }
    }

    public static void update() {
        for (Map.Entry<BlockSystem, BlockSystemRenderer> entry : RENDERERS.entrySet()) {
            entry.getValue().update();
        }
    }
}
