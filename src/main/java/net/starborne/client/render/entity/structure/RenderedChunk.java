package net.starborne.client.render.entity.structure;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.starborne.server.entity.structure.EntityChunk;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.StructurePlayerHandler;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RenderedChunk {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final TextureManager TEXTURE_MANAGER = MC.renderEngine;
    private static final BlockRendererDispatcher BLOCK_RENDERER_DISPATCHER = MC.getBlockRendererDispatcher();
    private static final BlockModelRenderer BLOCK_MODEL_RENDERER = BLOCK_RENDERER_DISPATCHER.getBlockModelRenderer();
    private final TextureAtlasSprite[] destroyStages = new TextureAtlasSprite[10];

    private final StructureEntity entity;
    private final EntityChunk chunk;
    private final VertexBuffer[] buffers = new VertexBuffer[BlockRenderLayer.values().length];
    private final net.minecraft.client.renderer.VertexBuffer[] builders = new net.minecraft.client.renderer.VertexBuffer[this.buffers.length];
    private final Map<TileEntity, TileEntitySpecialRenderer<TileEntity>> specialRenderers = new HashMap<>();
    private final ArrayDeque<TileEntity> specialRendererQueue = new ArrayDeque<>();
    private final ArrayDeque<TileEntity> specialRendererRemoveQueue = new ArrayDeque<>();

    private Set<BlockRenderLayer> rebuild = new HashSet<>();
    private final Object rebuildLock = new Object();
    private final Lock[] renderLayerLocks = new Lock[this.buffers.length];

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
        for (int i = 0; i < this.renderLayerLocks.length; i++) {
            this.renderLayerLocks[i] = new ReentrantLock();
        }
        TextureMap textureMap = MC.getTextureMapBlocks();
        for (int i = 0; i < this.destroyStages.length; ++i) {
            this.destroyStages[i] = textureMap.getAtlasSprite("minecraft:blocks/destroy_stage_" + i);
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
        while (this.rebuild.size() > 0) {
            synchronized (this.rebuildLock) {
                for (BlockRenderLayer layer : this.rebuild) {
                    this.rebuildLayerInternal(layer);
                }
                this.rebuild.clear();
            }
        }
    }

    public void render(float partialTicks) {
        Map<BlockPos, Integer> breaking = new HashMap<>();
        for (Map.Entry<EntityPlayer, StructurePlayerHandler> entry : this.entity.getPlayerHandlers().entrySet()) {
            StructurePlayerHandler handler = entry.getValue();
            BlockPos pos = handler.getBreaking();
            if (pos != null) {
                breaking.put(pos, (int) (handler.getBreakProgress() * 10.0F));
            }
        }
        for (Map.Entry<TileEntity, TileEntitySpecialRenderer<TileEntity>> renderer : this.specialRenderers.entrySet()) {
            TileEntity tile = renderer.getKey();
            TileEntitySpecialRenderer<TileEntity> value = renderer.getValue();
            BlockPos position = tile.getPos();
            value.renderTileEntityAt(tile, position.getX(), position.getY(), position.getZ(), partialTicks, -1);
            if (breaking.containsKey(position)) {
                value.renderTileEntityAt(tile, position.getX(), position.getY(), position.getZ(), partialTicks, breaking.get(position));
            }
            TEXTURE_MANAGER.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        }
        if (breaking.size() > 0) {
            Tessellator tessellator = Tessellator.getInstance();
            net.minecraft.client.renderer.VertexBuffer builder = tessellator.getBuffer();
            GlStateManager.enableBlend();
            GlStateManager.depthMask(false);
            TEXTURE_MANAGER.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F);
            GlStateManager.doPolygonOffset(-3.0F, -3.0F);
            GlStateManager.enablePolygonOffset();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.enableAlpha();
            GlStateManager.pushMatrix();
            RenderHelper.disableStandardItemLighting();
            builder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            builder.noColor();

            for (Map.Entry<BlockPos, Integer> entry : breaking.entrySet()) {
                BlockPos pos = entry.getKey();
                IBlockState state = this.entity.structureWorld.getBlockState(pos);
                Block block = state.getBlock();
                TileEntity tile = this.entity.structureWorld.getTileEntity(pos);
                boolean hasBreak = block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockSign || block instanceof BlockSkull;
                if (!hasBreak) {
                    hasBreak = tile != null && tile.canRenderBreaking();
                }
                if (!hasBreak) {
                    BLOCK_RENDERER_DISPATCHER.renderBlockDamage(state, pos, this.destroyStages[entry.getValue()], this.entity.structureWorld);
                }
            }

            tessellator.draw();
            GlStateManager.doPolygonOffset(0.0F, 0.0F);
            GlStateManager.disablePolygonOffset();
            GlStateManager.depthMask(true);
            RenderHelper.enableStandardItemLighting();
            GlStateManager.popMatrix();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            TEXTURE_MANAGER.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableAlpha();
        }
    }

    public void renderLayer(BlockRenderLayer layer) {
        if (layer == BlockRenderLayer.TRANSLUCENT) {
            GlStateManager.enableBlend();
        } else if (layer == BlockRenderLayer.CUTOUT_MIPPED) {
            TEXTURE_MANAGER.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        }
        Lock lock = this.renderLayerLocks[layer.ordinal()];
//        lock.lock();
        this.enableState();
        VertexBuffer buffer = this.buffers[layer.ordinal()];
        buffer.bindBuffer();
        this.bindAttributes();
        buffer.drawArrays(GL11.GL_QUADS);
        buffer.unbindBuffer();
        this.disableState();
//        lock.unlock();
        GlStateManager.resetColor();
        if (layer == BlockRenderLayer.TRANSLUCENT) {
            GlStateManager.disableBlend();
        } else if (layer == BlockRenderLayer.CUTOUT_MIPPED) {
            TEXTURE_MANAGER.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        }
    }

    public void rebuildLayer(BlockRenderLayer layer) {
        if (!this.rebuild.contains(layer)) {
            synchronized (this.rebuildLock) {
                this.rebuild.add(layer);
            }
        }
    }

    public void rebuild() {
        synchronized (this.rebuildLock) {
            for (BlockRenderLayer layer : BlockRenderLayer.values()) {
                if (!this.rebuild.contains(layer)) {
                    this.rebuild.add(layer);
                }
            }
        }
    }

    private void rebuildLayerInternal(BlockRenderLayer layer) {
        int index = layer.ordinal();
        VertexBuffer buffer = new VertexBuffer(DefaultVertexFormats.BLOCK);
        VertexBuffer prevBuffer = this.buffers[index];
        Lock lock = this.renderLayerLocks[layer.ordinal()];
//        lock.lock();
        buffer.bindBuffer();
        this.bindAttributes();
        this.drawLayer(layer, buffer);
        buffer.unbindBuffer();
        this.buffers[index] = buffer;
//        lock.unlock();
        if (prevBuffer != null) {
            prevBuffer.deleteGlBuffers();
        }
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
        BlockPos.MutableBlockPos globalPosition = new BlockPos.MutableBlockPos();
        BlockPos chunkPosition = this.chunk.getPosition();
        int offsetX = chunkPosition.getX() << 4;
        int offsetY = chunkPosition.getY() << 4;
        int offsetZ = chunkPosition.getZ() << 4;
        for (int blockX = 0; blockX < 16; blockX++) {
            for (int blockY = 0; blockY < 16; blockY++) {
                for (int blockZ = 0; blockZ < 16; blockZ++) {
                    IBlockState state = this.chunk.getBlockState(blockX, blockY, blockZ);
                    if (state.getBlock() != Blocks.AIR && state.getBlock().getBlockLayer() == layer) {
                        EnumBlockRenderType renderType = state.getRenderType();
                        if (renderType != EnumBlockRenderType.INVISIBLE) {
                            position.setPos(blockX, blockY, blockZ);
                            globalPosition.setPos(offsetX + blockX, offsetY + blockY, offsetZ + blockZ);
                            state = state.getActualState(this.entity.structureWorld, globalPosition);
                            builder.setTranslation(position.getX() - globalPosition.getX(), position.getY() - globalPosition.getY(), position.getZ() - globalPosition.getZ());
                            switch (renderType) {
                                case MODEL:
                                    IBakedModel model = BLOCK_RENDERER_DISPATCHER.getModelForState(state);
                                    state = state.getBlock().getExtendedState(state, this.entity.structureWorld, position);
                                    BLOCK_MODEL_RENDERER.renderModel(this.entity.structureWorld, model, state, globalPosition, builder, true);
                                    break;
                                case ENTITYBLOCK_ANIMATED:
                                    break;
                                case LIQUID:
                                    BLOCK_RENDERER_DISPATCHER.fluidRenderer.renderFluid(this.entity.structureWorld, state, globalPosition, builder);
                                    break;
                            }
                            builder.setTranslation(0, 0, 0);
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

    private void enableState() {
        GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glEnableClientState(GL11.GL_COLOR_ARRAY);
    }

    private void disableState() {
        GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY);
    }
}
