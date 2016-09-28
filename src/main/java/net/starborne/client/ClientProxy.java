package net.starborne.client;

import net.minecraftforge.common.MinecraftForge;
import net.starborne.client.render.RenderRegistry;
import net.starborne.server.ServerProxy;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.world.StructureWorld;
import net.starborne.server.entity.structure.world.StructureWorldClient;

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

    @Override
    public StructureWorld createStructureWorld(StructureEntity entity) {
        return entity.worldObj.isRemote ? new StructureWorldClient(entity) : super.createStructureWorld(entity);
    }
}
