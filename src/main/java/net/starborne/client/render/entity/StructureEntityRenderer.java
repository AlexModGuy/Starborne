package net.starborne.client.render.entity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.starborne.server.entity.structure.EntityChunk;
import net.starborne.server.entity.structure.StructureEntity;
import org.lwjgl.opengl.GL11;

import java.util.Map;

@SideOnly(Side.CLIENT)
public class StructureEntityRenderer extends Render<StructureEntity> {
    private static final Minecraft MC = Minecraft.getMinecraft();

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
        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer buffer = tessellator.getBuffer();
        if (this.renderOutlines) {
            GlStateManager.enableColorMaterial();
            GlStateManager.enableOutlineMode(this.getTeamColor(entity));
        }
        BlockRendererDispatcher rendererDispatcher = MC.getBlockRendererDispatcher();
        BlockModelRenderer blockModelRenderer = rendererDispatcher.getBlockModelRenderer();
        BlockPos position = new BlockPos(entity.posX, entity.getEntityBoundingBox().maxY, entity.posZ);
        for (Map.Entry<BlockPos, EntityChunk> entry : entity.getChunks().entrySet()) {
            EntityChunk chunk = entry.getValue();
            if (!chunk.isEmpty()) {
                BlockPos chunkPosition = chunk.getPosition();
                int chunkX = chunkPosition.getX() << 4;
                int chunkY = chunkPosition.getY() << 4;
                int chunkZ = chunkPosition.getZ() << 4;
                GlStateManager.pushMatrix();
                GlStateManager.translate(chunkX, chunkY, chunkZ);
                for (int blockX = 0; blockX < 16; blockX++) {
                    for (int blockY = 0; blockY < 16; blockY++) {
                        for (int blockZ = 0; blockZ < 16; blockZ++) {
                            IBlockState state = chunk.getBlockState(blockX, blockY, blockZ);
                            if (state.getBlock() != Blocks.AIR) {
                                GlStateManager.pushMatrix();
                                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                                GlStateManager.translate(blockX - 0.5, blockY, blockZ - 0.5);
                                blockModelRenderer.renderModel(entity, rendererDispatcher.getModelForState(state), state, BlockPos.ORIGIN, buffer, false, MathHelper.getPositionRandom(position));
                                tessellator.draw();
                                GlStateManager.popMatrix();
                            }
                        }
                    }
                }
                GlStateManager.popMatrix();
            }
        }
        if (this.renderOutlines) {
            GlStateManager.disableOutlineMode();
            GlStateManager.disableColorMaterial();
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
