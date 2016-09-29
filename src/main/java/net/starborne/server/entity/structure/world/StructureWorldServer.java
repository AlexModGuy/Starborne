package net.starborne.server.entity.structure.world;

import net.starborne.server.entity.structure.StructureEntity;

public class StructureWorldServer extends StructureWorld {
    public StructureWorldServer(StructureEntity entity) {
        super(entity);
        this.addEventListener(new ServerWorldListener(this));
    }
}
