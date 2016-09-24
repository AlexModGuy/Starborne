package net.starborne.server.entity.structure;

import net.minecraft.entity.player.EntityPlayerMP;
import net.starborne.Starborne;
import net.starborne.server.message.EntityChunkMessage;

import java.util.LinkedList;
import java.util.List;

public class EntityChunkTracker {
    private final StructureEntity entity;
    private final EntityPlayerMP player;
    private List<EntityChunk> dirty = new LinkedList<>();

    public EntityChunkTracker(StructureEntity entity, EntityPlayerMP player) {
        this.entity = entity;
        this.player = player;
    }

    public void update() {
        if (this.dirty.size() > 0) {
            Starborne.networkWrapper.sendTo(new EntityChunkMessage(this.entity.getEntityId(), this.dirty.get(0)), this.player);
            this.dirty.remove(0);
        }
    }

    public void setDirty(EntityChunk chunk) {
        if (!this.dirty.contains(chunk)) {
            this.dirty.add(chunk);
        }
    }
}
