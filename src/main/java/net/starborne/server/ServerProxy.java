package net.starborne.server;

import net.minecraftforge.common.MinecraftForge;
import net.starborne.server.block.BlockRegistry;
import net.starborne.server.item.ItemRegistry;

public class ServerProxy {
    public void onPreInit() {
        BlockRegistry.onPreInit();
        ItemRegistry.onPreInit();

        MinecraftForge.EVENT_BUS.register(new ServerEventHandler());
    }

    public void onInit() {

    }

    public void onPostInit() {

    }
}
