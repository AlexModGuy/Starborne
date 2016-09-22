package net.starborne.server.entity.structure;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.starborne.client.render.entity.structure.RenderedChunk;

public class ClientEntityChunk extends EntityChunk {
    protected RenderedChunk renderedChunk;

    public ClientEntityChunk(StructureEntity entity, BlockPos position) {
        super(entity, position);
        this.renderedChunk = new RenderedChunk(entity, this);
    }

    @Override
    public boolean setBlockState(int x, int y, int z, IBlockState state) {
        if (super.setBlockState(x, y, z, state)) {
            this.renderedChunk.rebuildLayer(state.getBlock().getBlockLayer());
            return true;
        }
        return false;
    }

    @Override
    public void deserialize(NBTTagCompound compound) {
        super.deserialize(compound);
        this.renderedChunk.rebuild();
    }

    @Override
    public void unload() {
        super.unload();
        this.renderedChunk.delete();
    }

    public RenderedChunk getRenderedChunk() {
        return this.renderedChunk;
    }
}
