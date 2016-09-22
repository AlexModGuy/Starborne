package net.starborne.client.render.entity.structure;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.starborne.server.entity.structure.EntityChunk;
import net.starborne.server.entity.structure.StructureEntity;
import org.lwjgl.opengl.GL11;

public class RenderedChunk {
    private static final BlockRendererDispatcher BLOCK_RENDERER_DISPATCHER = Minecraft.getMinecraft().getBlockRendererDispatcher();
    private static final BlockModelRenderer BLOCK_MODEL_RENDERER = BLOCK_RENDERER_DISPATCHER.getBlockModelRenderer();

    private final StructureEntity entity;
    private final EntityChunk chunk;
    private final VertexBuffer[] buffers = new VertexBuffer[BlockRenderLayer.values().length];
    private final net.minecraft.client.renderer.VertexBuffer[] builders = new net.minecraft.client.renderer.VertexBuffer[this.buffers.length];

    public RenderedChunk(StructureEntity entity, EntityChunk chunk) {
        this.entity = entity;
        this.chunk = chunk;
        this.setBuilder(BlockRenderLayer.SOLID, 0x20000);
        this.setBuilder(BlockRenderLayer.CUTOUT, 0x2000);
        this.setBuilder(BlockRenderLayer.CUTOUT_MIPPED, 0x2000);
        this.setBuilder(BlockRenderLayer.TRANSLUCENT, 0x4000);
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            this.rebuildLayer(layer);
        }
    }

    private void setBuilder(BlockRenderLayer layer, int size) {
        this.builders[layer.ordinal()] = new net.minecraft.client.renderer.VertexBuffer(size);
    }

    public void update() {
    }

    public void render() {
        for (BlockRenderLayer renderLayer : BlockRenderLayer.values()) {
            this.renderLayer(renderLayer);
        }
    }

    public void renderLayer(BlockRenderLayer layer) {
        if (layer == BlockRenderLayer.TRANSLUCENT) {
            GlStateManager.enableBlend();
        }
        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glEnableClientState(GL11.GL_COLOR_ARRAY);
        VertexBuffer buffer = this.buffers[layer.ordinal()];
        buffer.bindBuffer();
        this.bindAttributes();
        buffer.drawArrays(GL11.GL_QUADS);
        buffer.unbindBuffer();
        GlStateManager.resetColor();
        GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY);
        if (layer == BlockRenderLayer.TRANSLUCENT) {
            GlStateManager.disableBlend();
        }
    }

    public void rebuildLayer(BlockRenderLayer layer) {
        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        int index = layer.ordinal();
        VertexBuffer buffer = this.buffers[index];
        if (buffer != null) {
            buffer.deleteGlBuffers();
        }
        buffer = new VertexBuffer(DefaultVertexFormats.BLOCK);
        buffer.bindBuffer();
        this.bindAttributes();
        this.drawLayer(layer, buffer);
        buffer.unbindBuffer();
        this.buffers[index] = buffer;
        GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
    }

    private void bindAttributes() {
        GlStateManager.glVertexPointer(3, GL11.GL_FLOAT, 28, 0);
        GlStateManager.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 28, 12);
        GlStateManager.glTexCoordPointer(2, GL11.GL_FLOAT, 28, 16);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.glTexCoordPointer(2, GL11.GL_SHORT, 28, 24);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private void drawLayer(BlockRenderLayer layer, VertexBuffer buffer) {
        net.minecraft.client.renderer.VertexBuffer builder = this.builders[layer.ordinal()];
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (int blockX = 0; blockX < 16; blockX++) {
            for (int blockY = 0; blockY < 16; blockY++) {
                for (int blockZ = 0; blockZ < 16; blockZ++) {
                    IBlockState state = this.chunk.getBlockState(blockX, blockY, blockZ);
                    if (state.getBlock() != Blocks.AIR && state.getBlock().getBlockLayer() == layer) {
                        EnumBlockRenderType renderType = state.getRenderType();
                        if (renderType != EnumBlockRenderType.INVISIBLE) {
                            position.setPos(blockX, blockY, blockZ);
                            state = state.getActualState(this.entity, position);
                            switch (renderType) {
                                case MODEL:
                                    IBakedModel model = BLOCK_RENDERER_DISPATCHER.getModelForState(state);
                                    state = state.getBlock().getExtendedState(state, this.entity, position);
                                    BLOCK_MODEL_RENDERER.renderModel(this.entity, model, state, position, builder, true);
                                    break;
                                case ENTITYBLOCK_ANIMATED:
                                    break;
                                case LIQUID:
                                    BLOCK_RENDERER_DISPATCHER.fluidRenderer.renderFluid(this.entity, state, position, builder);
                                    break;
                            }
                        }
                    }
                }
            }
        }
        builder.finishDrawing();
        buffer.bufferData(builder.getByteBuffer());
    }

    public void delete() {
        for (VertexBuffer buffer : this.buffers) {
            buffer.deleteGlBuffers();
        }
    }

    public void rebuild() {
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            this.rebuildLayer(layer);
        }
    }
}
