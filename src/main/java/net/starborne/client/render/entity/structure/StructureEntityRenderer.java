package net.starborne.client.render.entity.structure;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.starborne.client.ClientEventHandler;
import net.starborne.server.entity.structure.ClientEntityChunk;
import net.starborne.server.entity.structure.EntityChunk;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.StructurePlayerHandler;

import java.util.Map;

@SideOnly(Side.CLIENT)
public class StructureEntityRenderer extends Render<StructureEntity> {
    public StructureEntityRenderer(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(StructureEntity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        this.bindEntityTexture(entity);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(entityYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(entity.rotationRoll, 0.0F, 0.0F, 1.0F);
        GlStateManager.disableLighting();
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            for (Map.Entry<BlockPos, EntityChunk> entry : entity.structureWorld.getChunks().entrySet()) {
                EntityChunk chunk = entry.getValue();
                if (!chunk.isEmpty()) {
                    BlockPos chunkPosition = chunk.getPosition();
                    int chunkX = chunkPosition.getX() << 4;
                    int chunkY = chunkPosition.getY() << 4;
                    int chunkZ = chunkPosition.getZ() << 4;
                    if (chunk instanceof ClientEntityChunk) {
                        ClientEntityChunk clientChunk = (ClientEntityChunk) chunk;
                        GlStateManager.pushMatrix();
                        GlStateManager.translate(chunkX - 0.5, chunkY, chunkZ - 0.5);
                        clientChunk.getRenderedChunk().renderLayer(layer);
                        GlStateManager.popMatrix();
                    }
                }
            }
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(-0.5, 0.0, -0.5);
        for (Map.Entry<BlockPos, EntityChunk> entry : entity.structureWorld.getChunks().entrySet()) {
            EntityChunk chunk = entry.getValue();
            if (!chunk.isEmpty()) {
                if (chunk instanceof ClientEntityChunk) {
                    ClientEntityChunk clientChunk = (ClientEntityChunk) chunk;
                    clientChunk.getRenderedChunk().render(partialTicks);
                }
            }
        }
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
        if (ClientEventHandler.mousedOver == entity) {
            StructurePlayerHandler handler = ClientEventHandler.handlers.get(entity);
            if (handler != null) {
                RayTraceResult result = handler.getMouseOver();
                BlockPos pos = result.getBlockPos();
                GlStateManager.translate(-0.5, 0.0, -0.5);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.glLineWidth(2.0F);
                GlStateManager.disableTexture2D();
                GlStateManager.depthMask(false);
                IBlockState state = entity.structureWorld.getBlockState(pos);
                if (state.getMaterial() != Material.AIR) {
                    RenderGlobal.drawSelectionBoundingBox(state.getSelectedBoundingBox(entity.structureWorld, pos).expandXyz(0.002), 0.0F, 0.0F, 0.0F, 0.4F);
                }
                GlStateManager.depthMask(true);
                GlStateManager.enableTexture2D();
                GlStateManager.disableBlend();
            }
        }
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(StructureEntity entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
