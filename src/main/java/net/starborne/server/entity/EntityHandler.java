package net.starborne.server.entity;

import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.starborne.Starborne;

public class EntityHandler {
    public static void onPreInit() {
        EntityRegistry.registerModEntity(BlockSystemControlEntity.class, "block_system_control", 0, Starborne.INSTANCE, 1024, 1, true);
    }
}
