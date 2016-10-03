package net.starborne.server.entity.structure;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.starborne.client.render.entity.structure.RenderedChunk;

public class ClientEntityChunk extends EntityChunk {
    protected RenderedChunk renderedChunk;

    public ClientEntityChunk(StructureEntity entity, BlockPos position) {
        super(entity, position);
        this.renderedChunk = new RenderedChunk(entity, this);
    }

    @Override
    public boolean setBlockState(int x, int y, int z, IBlockState state) {
        boolean placed = super.setBlockState(x, y, z, state);
        if (!this.loading) {
            this.renderedChunk.rebuild();
            this.rebuildNeighbours();
        }
        return placed;
    }

    private void rebuildNeighbours() {
        for (EnumFacing facing : EnumFacing.values()) {
            EntityChunk chunk = this.entity.structureWorld.getChunk(this.position.offset(facing));
            if (chunk instanceof ClientEntityChunk && !chunk.isEmpty()) {
                ((ClientEntityChunk) chunk).rebuild();
            }
        }
    }

    @Override
    public void deserialize(NBTTagCompound compound) {
        super.deserialize(compound);
        this.renderedChunk.rebuild();
        this.rebuildNeighbours();
    }

    @Override
    public void unload() {
        super.unload();
        this.renderedChunk.delete();
    }

    @Override
    public void update() {
        super.update();
        this.renderedChunk.update();
    }

    @Override
    protected void addTileEntity(TileEntity tileEntity, BlockPos position) {
        super.addTileEntity(tileEntity, position);
        this.renderedChunk.addTileEntity(tileEntity);
    }

    @Override
    protected void removeTileEntity(BlockPos position) {
        TileEntity tile = this.getTileEntity(position);
        if (tile != null) {
            this.renderedChunk.removeTileEntity(tile);
        }
        super.removeTileEntity(position);
    }

    public void rebuild() {
        this.renderedChunk.rebuild();
    }

    public RenderedChunk getRenderedChunk() {
        return this.renderedChunk;
    }
}
