package net.starborne.client.render.blocksystem;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.starborne.Starborne;
import net.starborne.server.blocksystem.BlockSystem;
import net.starborne.server.blocksystem.BlockSystemPlayerHandler;
import net.starborne.server.blocksystem.ServerBlockSystemHandler;

@SideOnly(Side.CLIENT)
public class BlockSystemRenderer {
    private static final Minecraft MC = Minecraft.getMinecraft();

    private BlockSystem blockSystem;

    private double frustumUpdatePosX = Double.MIN_VALUE;
    private double frustumUpdatePosY = Double.MIN_VALUE;
    private double frustumUpdatePosZ = Double.MIN_VALUE;
    private int frustumUpdatePosChunkX = Integer.MIN_VALUE;
    private int frustumUpdatePosChunkY = Integer.MIN_VALUE;
    private int frustumUpdatePosChunkZ = Integer.MIN_VALUE;

    private BlockSystemViewFrustum viewFrustum;

    private int viewDistance;

    public BlockSystemRenderer(BlockSystem blockSystem) {
        this.blockSystem = blockSystem;
        this.viewFrustum = new BlockSystemViewFrustum(this, blockSystem, MC.gameSettings.renderDistanceChunks);
        this.viewDistance = MC.gameSettings.renderDistanceChunks;
    }

    public void renderBlockSystem(Entity viewEntity, double x, double y, double z, float rotationX, float rotationY, float rotationZ, float partialTicks) {
        int viewDistance = MC.gameSettings.renderDistanceChunks;
        if (this.viewDistance != viewDistance) {
            this.viewDistance = viewDistance;
            this.viewFrustum.delete();
            this.viewFrustum = new BlockSystemViewFrustum(this, this.blockSystem, viewDistance);
        }

        double deltaX = viewEntity.posX - this.frustumUpdatePosX;
        double deltaY = viewEntity.posY - this.frustumUpdatePosY;
        double deltaZ = viewEntity.posZ - this.frustumUpdatePosZ;
        double delta = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

        if (this.frustumUpdatePosChunkX != viewEntity.chunkCoordX || this.frustumUpdatePosChunkY != viewEntity.chunkCoordY || this.frustumUpdatePosChunkZ != viewEntity.chunkCoordZ || delta > 16.0D) {
            this.frustumUpdatePosX = viewEntity.posX;
            this.frustumUpdatePosY = viewEntity.posY;
            this.frustumUpdatePosZ = viewEntity.posZ;
            this.frustumUpdatePosChunkX = viewEntity.chunkCoordX;
            this.frustumUpdatePosChunkY = viewEntity.chunkCoordY;
            this.frustumUpdatePosChunkZ = viewEntity.chunkCoordZ;
            this.viewFrustum.updateChunkPositions(viewEntity.posX, viewEntity.posZ);
        }

        for (RenderedChunk chunk : this.viewFrustum.chunks) {
            if (chunk.doesRequireUpdate()) {
                chunk.rebuild();
                chunk.clearNeedsUpdate();
            }
        }

        MC.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(rotationY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rotationX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(rotationZ, 0.0F, 0.0F, 1.0F);
        GlStateManager.disableLighting();
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            for (RenderedChunk chunk : this.viewFrustum.chunks) {
                if (!chunk.isEmpty()) {
                    BlockPos chunkPosition = chunk.getPosition();
                    int chunkX = chunkPosition.getX() << 4;
                    int chunkY = chunkPosition.getY() << 4;
                    int chunkZ = chunkPosition.getZ() << 4;
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(chunkX - 0.5, chunkY, chunkZ - 0.5);
                    chunk.renderLayer(layer);
                    GlStateManager.popMatrix();
                }
            }
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(-0.5, 0.0, -0.5);
        for (RenderedChunk chunk : this.viewFrustum.chunks) {
            chunk.render(partialTicks);
        }
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
        ServerBlockSystemHandler structureHandler = Starborne.PROXY.getBlockSystemHandler(this.blockSystem.getMainWorld());
        if (structureHandler.getMousedOver(MC.thePlayer) == this.blockSystem) {
            BlockSystemPlayerHandler handler = structureHandler.get(this.blockSystem, MC.thePlayer);
            if (handler != null) {
                RayTraceResult result = handler.getMouseOver();
                BlockPos pos = result.getBlockPos();
                GlStateManager.translate(-0.5, 0.0, -0.5);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                GlStateManager.glLineWidth(2.0F);
                GlStateManager.disableTexture2D();
                GlStateManager.depthMask(false);
                IBlockState state = this.blockSystem.getBlockState(pos);
                if (state.getMaterial() != Material.AIR) {
                    RenderGlobal.drawSelectionBoundingBox(state.getSelectedBoundingBox(this.blockSystem, pos).expandXyz(0.002), 0.0F, 0.0F, 0.0F, 0.4F);
                }
                GlStateManager.depthMask(true);
                GlStateManager.enableTexture2D();
                GlStateManager.disableBlend();
            }
        }
        GlStateManager.popMatrix();
    }

    public void queueRenderUpdate(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean requiresUpdate) {
        this.viewFrustum.queueRenderUpdate(minX, minY, minZ, maxX, maxY, maxZ, requiresUpdate);
    }

    public void deleteChunk(int xPosition, int zPosition) {
        int x = xPosition << 4;
        int z = zPosition << 4;
        this.queueRenderUpdate(x, 0, z, x + 16, 256, z + 16, true);
    }

    public void queueChunkRenderUpdate(int xPosition, int zPosition) {
        int x = xPosition << 4;
        int z = zPosition << 4;
        this.queueRenderUpdate(x, 0, z, x + 16, 256, z + 16, false);
    }

    public void update() {
        for (RenderedChunk chunk : this.viewFrustum.chunks) {
            chunk.update();
        }
    }
}
