package net.starborne.server.entity.structure;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.BlockStateContainer;
import net.starborne.Starborne;
import net.starborne.server.message.EntityChunkMessage;
import net.starborne.server.message.SetEntityBlockMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EntityChunkTracker {
    private final StructureEntity entity;
    private final EntityPlayerMP player;
    private List<EntityChunk> dirty = new LinkedList<>();
    private Map<EntityChunk, BlockStateContainer> lastData = new HashMap<>();

    public EntityChunkTracker(StructureEntity entity, EntityPlayerMP player) {
        this.entity = entity;
        this.player = player;
    }

    public void update() {
        if (this.dirty.size() > 0) {
            EntityChunk chunk = this.dirty.get(0);
            BlockStateContainer newLast = new BlockStateContainer();
            BlockStateContainer difference = new BlockStateContainer();
            BlockStateContainer last = this.lastData.get(chunk);
            List<BlockPos> differences = new ArrayList<>();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        IBlockState state = chunk.getBlockState(x, y, z);
                        newLast.set(x, y, z, state);
                        if (last == null || last.get(x, y, z) != state) {
                            difference.set(x, y, z, state);
                            differences.add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
            if (differences.size() > 0) {
                if (differences.size() == 1) {
                    BlockPos chunkPos = chunk.getPosition();
                    BlockPos globalPosition = differences.get(0).add(chunkPos.getX() << 4, chunkPos.getY() << 4, chunkPos.getZ() << 4);
                    Starborne.networkWrapper.sendTo(new SetEntityBlockMessage(this.entity.getEntityId(), globalPosition, chunk.getBlockState(differences.get(0))), this.player);
                } else {
                    Starborne.networkWrapper.sendTo(new EntityChunkMessage(this.entity.getEntityId(), chunk), this.player);
                }
            }
            this.lastData.put(chunk, newLast);
            this.dirty.remove(0);
        }
    }

    public void setDirty(EntityChunk chunk) {
        if (!this.dirty.contains(chunk)) {
            this.dirty.add(chunk);
        }
    }

    public void remove(EntityChunk chunk) {
        this.lastData.remove(chunk);
    }
}
