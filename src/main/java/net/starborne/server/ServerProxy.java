package net.starborne.server;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.starborne.server.biome.BiomeHandler;
import net.starborne.server.block.BlockRegistry;
import net.starborne.server.dimension.DimensionHandler;
import net.starborne.server.entity.EntityHandler;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.world.StructureWorld;
import net.starborne.server.entity.structure.world.StructureWorldServer;
import net.starborne.server.item.ItemRegistry;
import net.starborne.server.message.BaseMessage;

public class ServerProxy {
    public static final ServerStructureHandler STRUCTURE_HANDLER = new ServerStructureHandler();

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
        return new StructureWorldServer(entity);
    }

    public void playSound(ISound sound) {
    }

    public void pickBlock(EntityPlayer player, RayTraceResult mouseOver, TileEntity tile, World world, IBlockState state) {
    }

    public void scheduleTask(MessageContext context, Runnable runnable) {
        WorldServer world = (WorldServer) context.getServerHandler().playerEntity.worldObj;
        world.addScheduledTask(runnable);
    }

    public void handleMessage(BaseMessage message, MessageContext context) {
        EntityPlayerMP player = context.getServerHandler().playerEntity;
        this.scheduleTask(context, () -> message.onReceiveServer(player.getServer(), (WorldServer) player.worldObj, player, context));
    }

    public ServerStructureHandler getStructureHandler(World world) {
        return STRUCTURE_HANDLER;
    }
}
