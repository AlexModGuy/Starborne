package net.starborne.client.render.blocksystem;

import com.google.common.collect.Lists;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;

import java.util.List;

public class CompiledBlockSystemChunk {
    private final boolean[] layersUsed = new boolean[BlockRenderLayer.values().length];
    private final List<TileEntity> blockEntities = Lists.newArrayList();
    private boolean empty = true;

    public boolean isEmpty() {
        return this.empty;
    }

    protected void setLayerUsed(BlockRenderLayer layer) {
        this.empty = false;
        this.layersUsed[layer.ordinal()] = true;
    }

    public boolean isLayerEmpty(BlockRenderLayer layer) {
        return !this.layersUsed[layer.ordinal()];
    }

    public List<TileEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public void addBlockEntity(TileEntity entity) {
        this.blockEntities.add(entity);
    }
}
