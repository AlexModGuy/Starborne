package net.starborne.server;

import net.minecraftforge.common.MinecraftForge;

public class ServerProxy {
    public void onPreInit() {
        MinecraftForge.EVENT_BUS.register(new ServerEventHandler());
    }

    public void onInit() {

    }

    public void onPostInit() {

    }
}
