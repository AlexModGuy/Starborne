package net.starborne.server.entity;

import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.starborne.Starborne;
import net.starborne.server.entity.structure.StructureEntity;

public class EntityHandler {
    public static void onPreInit() {
        EntityRegistry.registerModEntity(StructureEntity.class, "structure_entity", 0, Starborne.MODID, 1024, 1, true);
    }
}
