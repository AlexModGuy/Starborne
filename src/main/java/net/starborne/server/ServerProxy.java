package net.starborne.server;

import net.minecraftforge.common.MinecraftForge;
import net.starborne.server.biome.BiomeHandler;
import net.starborne.server.block.BlockRegistry;
import net.starborne.server.dimension.DimensionHandler;
import net.starborne.server.entity.EntityHandler;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.world.StructureWorld;
import net.starborne.server.item.ItemRegistry;

public class ServerProxy {
    public void onPreInit() {
        BlockRegistry.onPreInit();
        ItemRegistry.onPreInit();
        DimensionHandler.onPreInit();
        BiomeHandler.onPreInit();
        EntityHandler.onPreInit();

        MinecraftForge.EVENT_BUS.register(new ServerEventHandler());
    }

    public void onInit() {

    }

    public void onPostInit() {

    }

    public StructureWorld createStructureWorld(StructureEntity entity) {
        return new StructureWorld(entity);
    }
}
