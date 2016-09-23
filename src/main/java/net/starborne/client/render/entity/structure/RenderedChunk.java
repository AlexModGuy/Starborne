package net.starborne.client.render.entity.structure;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.starborne.server.entity.structure.EntityChunk;
import net.starborne.server.entity.structure.StructureEntity;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class RenderedChunk {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final TextureManager TEXTURE_MANAGER = MC.renderEngine;
    private static final BlockRendererDispatcher BLOCK_RENDERER_DISPATCHER = MC.getBlockRendererDispatcher();
    private static final BlockModelRenderer BLOCK_MODEL_RENDERER = BLOCK_RENDERER_DISPATCHER.getBlockModelRenderer();

    private final StructureEntity entity;
    private final EntityChunk chunk;
    private final VertexBuffer[] buffers = new VertexBuffer[BlockRenderLayer.values().length];
    private final net.minecraft.client.renderer.VertexBuffer[] builders = new net.minecraft.client.renderer.VertexBuffer[this.buffers.length];
    private final Map<TileEntity, TileEntitySpecialRenderer<TileEntity>> specialRenderers = new HashMap<>();
    private final ArrayDeque<TileEntity> specialRendererQueue = new ArrayDeque<>();
    private final ArrayDeque<TileEntity> specialRendererRemoveQueue = new ArrayDeque<>();

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

    public void addTileEntity(TileEntity tileEntity) {
        TileEntitySpecialRenderer<TileEntity> renderer = TileEntityRendererDispatcher.instance.getSpecialRenderer(tileEntity);
        if (renderer != null) {
            this.specialRendererQueue.add(tileEntity);
        }
    }

    public void removeTileEntity(TileEntity tileEntity) {
        this.specialRendererRemoveQueue.add(tileEntity);
    }

    private void setBuilder(BlockRenderLayer layer, int size) {
        this.builders[layer.ordinal()] = new net.minecraft.client.renderer.VertexBuffer(size);
    }

    public void update() {
        while (this.specialRendererQueue.size() > 0) {
            TileEntity tile = this.specialRendererQueue.poll();
            this.specialRenderers.put(tile, TileEntityRendererDispatcher.instance.getSpecialRenderer(tile));
        }
        while (this.specialRendererRemoveQueue.size() > 0) {
            TileEntity tile = this.specialRendererRemoveQueue.poll();
            this.specialRenderers.remove(tile);
        }
    }

    public void render(float partialTicks) {
        for (BlockRenderLayer renderLayer : BlockRenderLayer.values()) {
            this.renderLayer(renderLayer);
        }
        for (Map.Entry<TileEntity, TileEntitySpecialRenderer<TileEntity>> renderer : this.specialRenderers.entrySet()) {
            TileEntity tile = renderer.getKey();
            TileEntitySpecialRenderer<TileEntity> value = renderer.getValue();
            BlockPos position = tile.getPos();
            value.renderTileEntityAt(tile, position.getX() & 15, position.getY() & 15, position.getZ() & 15, partialTicks, -1);
            TEXTURE_MANAGER.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
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
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY);
        if (layer == BlockRenderLayer.TRANSLUCENT) {
            GlStateManager.disableBlend();
        }
    }

    public void rebuildLayer(BlockRenderLayer layer) {
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
