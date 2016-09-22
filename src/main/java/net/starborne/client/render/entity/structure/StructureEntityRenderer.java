package net.starborne.client.render.entity.structure;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.starborne.server.entity.structure.ClientEntityChunk;
import net.starborne.server.entity.structure.EntityChunk;
import net.starborne.server.entity.structure.StructureEntity;

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
        for (Map.Entry<BlockPos, EntityChunk> entry : entity.getChunks().entrySet()) {
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
                    clientChunk.getRenderedChunk().render();
                    GlStateManager.popMatrix();
                }
            }
        }
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(StructureEntity entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
