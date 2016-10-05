package net.starborne.client.render.blocksystem;

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
import net.minecraft.world.ChunkCache;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.starborne.server.blocksystem.BlockSystem;
import net.starborne.server.blocksystem.BlockSystemPlayerHandler;
import org.lwjgl.opengl.GL11;

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

    private final BlockSystem blockSystem;
    private final VertexBuffer[] buffers = new VertexBuffer[BlockRenderLayer.values().length];
    private final net.minecraft.client.renderer.VertexBuffer[] builders = new net.minecraft.client.renderer.VertexBuffer[this.buffers.length];

    private ChunkCache region;

    private boolean requiresUpdate = true;
    private boolean requiresUpdateCustom;

    private BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(-1, -1, -1);

    private Set<BlockRenderLayer> rebuild = new HashSet<>();
    private final Object rebuildLock = new Object();
    private final Lock[] renderLayerLocks = new Lock[this.buffers.length];

    private CompiledBlockSystemChunk compiledChunk = new CompiledBlockSystemChunk();

    public RenderedChunk(BlockSystem blockSystem) {
        this.blockSystem = blockSystem;
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

    private void setBuilder(BlockRenderLayer layer, int size) {
        this.builders[layer.ordinal()] = new net.minecraft.client.renderer.VertexBuffer(size);
    }

    private void deleteLayer(BlockRenderLayer layer) {
        this.builders[layer.ordinal()] = null;
    }

    public void update() {
        while (this.rebuild.size() > 0) {
            this.compiledChunk = new CompiledBlockSystemChunk();
            BlockPos topCorner = this.position.add(16, 16, 16);
            for (BlockPos.MutableBlockPos pos : BlockPos.getAllInBoxMutable(this.position, topCorner)) {
                IBlockState state = this.region.getBlockState(pos);
                Block block = state.getBlock();
                if (block.hasTileEntity(state)) {
                    TileEntity blockEntity = this.region.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
                    if (blockEntity != null) {
                        TileEntitySpecialRenderer<TileEntity> specialRenderer = TileEntityRendererDispatcher.instance.getSpecialRenderer(blockEntity);
                        if (specialRenderer != null) {
                            this.compiledChunk.addBlockEntity(blockEntity);
                        }
                    }
                }
                for (BlockRenderLayer layer : BlockRenderLayer.values()) {
                    if (block.canRenderInLayer(state, layer)) {
                        ForgeHooksClient.setRenderLayer(layer);
                        if (state.getRenderType() != EnumBlockRenderType.INVISIBLE) {
                            this.compiledChunk.setLayerUsed(layer);
                            break;
                        }
                    }
                }
                ForgeHooksClient.setRenderLayer(null);
            }
            for (BlockRenderLayer layer : BlockRenderLayer.values()) {
                if (this.compiledChunk.isLayerEmpty(layer)) {
                    this.deleteLayer(layer);
                } else {
                    int size = 0x2000;
                    if (layer == BlockRenderLayer.SOLID) {
                        size = 0x20000;
                    } else if (layer == BlockRenderLayer.TRANSLUCENT) {
                        size = 0x4000;
                    }
                    this.setBuilder(layer, size);
                }
            }
            synchronized (this.rebuildLock) {
                for (BlockRenderLayer layer : this.rebuild) {
                    if (!this.compiledChunk.isLayerEmpty(layer)) {
                        this.rebuildLayerInternal(layer);
                    }
                }
                this.rebuild.clear();
            }
        }
    }

    public void render(float partialTicks) {
        Map<BlockPos, Integer> breaking = new HashMap<>();
        for (Map.Entry<EntityPlayer, BlockSystemPlayerHandler> entry : this.blockSystem.getPlayerHandlers().entrySet()) {
            BlockSystemPlayerHandler handler = entry.getValue();
            BlockPos pos = handler.getBreaking();
            if (pos != null) {
                breaking.put(pos, (int) (handler.getBreakProgress() * 10.0F));
            }
        }
        for (TileEntity blockEntity : this.compiledChunk.getBlockEntities()) {
            TileEntitySpecialRenderer<TileEntity> renderer = TileEntityRendererDispatcher.instance.getSpecialRenderer(blockEntity);
            BlockPos position = blockEntity.getPos();
            renderer.renderTileEntityAt(blockEntity, position.getX(), position.getY(), position.getZ(), partialTicks, -1);
            if (breaking.containsKey(position)) {
                renderer.renderTileEntityAt(blockEntity, position.getX(), position.getY(), position.getZ(), partialTicks, breaking.get(position));
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
                IBlockState state = this.blockSystem.getBlockState(pos);
                Block block = state.getBlock();
                TileEntity tile = this.blockSystem.getTileEntity(pos);
                boolean hasBreak = block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockSign || block instanceof BlockSkull;
                if (!hasBreak) {
                    hasBreak = tile != null && tile.canRenderBreaking();
                }
                if (!hasBreak) {
                    BLOCK_RENDERER_DISPATCHER.renderBlockDamage(state, pos, this.destroyStages[entry.getValue()], this.blockSystem);
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
        if (!this.compiledChunk.isEmpty() && !this.compiledChunk.isLayerEmpty(layer)) {
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
    }

    public void rebuildLayer(BlockRenderLayer layer) {
        this.updateChunkCache();
        if (!this.rebuild.contains(layer)) {
            synchronized (this.rebuildLock) {
                this.rebuild.add(layer);
            }
        }
    }

    public void rebuild() {
        this.updateChunkCache();
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
        ForgeHooksClient.setRenderLayer(layer);
        net.minecraft.client.renderer.VertexBuffer builder = this.builders[layer.ordinal()];
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos globalPosition = new BlockPos.MutableBlockPos();
        int offsetX = this.position.getX() << 4;
        int offsetY = this.position.getY() << 4;
        int offsetZ = this.position.getZ() << 4;
        for (int blockX = 0; blockX < 16; blockX++) {
            for (int blockY = 0; blockY < 16; blockY++) {
                for (int blockZ = 0; blockZ < 16; blockZ++) {
                    globalPosition.setPos(offsetX + blockX, offsetY + blockY, offsetZ + blockZ);
                    IBlockState state = this.region.getBlockState(globalPosition);
                    if (state.getBlock() != Blocks.AIR && state.getBlock().canRenderInLayer(state, layer)) {
                        EnumBlockRenderType renderType = state.getRenderType();
                        if (renderType != EnumBlockRenderType.INVISIBLE) {
                            position.setPos(blockX, blockY, blockZ);
                            state = state.getActualState(this.blockSystem, globalPosition);
                            builder.setTranslation(position.getX() - globalPosition.getX(), position.getY() - globalPosition.getY(), position.getZ() - globalPosition.getZ());
                            switch (renderType) {
                                case MODEL:
                                    IBakedModel model = BLOCK_RENDERER_DISPATCHER.getModelForState(state);
                                    state = state.getBlock().getExtendedState(state, this.blockSystem, position);
                                    BLOCK_MODEL_RENDERER.renderModel(this.blockSystem, model, state, globalPosition, builder, true);
                                    break;
                                case ENTITYBLOCK_ANIMATED:
                                    break;
                                case LIQUID:
                                    BLOCK_RENDERER_DISPATCHER.fluidRenderer.renderFluid(this.blockSystem, state, globalPosition, builder);
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

    public void setPosition(int x, int y, int z) {
        if (x != this.position.getX() || y != this.position.getY() || z != this.position.getZ()) {
            this.compiledChunk = new CompiledBlockSystemChunk();
            this.rebuild.clear();
            this.position.setPos(x, y, z);
        }
    }

    public BlockPos getPosition() {
        return this.position;
    }

    public void setRequiresUpdate(boolean requiresUpdate) {
        if (this.requiresUpdate) {
            requiresUpdate |= this.requiresUpdateCustom;
        }
        this.requiresUpdate = true;
        this.requiresUpdateCustom = requiresUpdate;
    }

    public void clearNeedsUpdate() {
        this.requiresUpdate = false;
        this.requiresUpdateCustom = false;
    }

    public boolean doesRequireUpdate() {
        return this.requiresUpdate;
    }

    public boolean doesRequireCustomUpdate() {
        return this.requiresUpdate && this.requiresUpdateCustom;
    }

    public boolean isEmpty() {
        return this.compiledChunk.isEmpty();
    }

    private void updateChunkCache() {
        ChunkCache cache = new ChunkCache(this.blockSystem, this.position.add(-1, -1, -1), this.position.add(16, 16, 16), 1);
        MinecraftForgeClient.onRebuildChunk(this.blockSystem, this.position, cache);
        this.region = cache;
    }
}
