package net.starborne.client;

import net.minecraftforge.common.MinecraftForge;
import net.starborne.client.render.RenderRegistry;
import net.starborne.server.ServerProxy;

public class ClientProxy extends ServerProxy {
    @Override
    public void onPreInit() {
        super.onPreInit();
        RenderRegistry.onPreInit();
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
    }

    @Override
    public void onInit() {
        super.onInit();
        RenderRegistry.onInit();
    }

    @Override
    public void onPostInit() {
        super.onPostInit();
        RenderRegistry.onPostInit();
    }
}
