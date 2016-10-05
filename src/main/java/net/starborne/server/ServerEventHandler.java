package net.starborne.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.starborne.Starborne;
import net.starborne.server.blocksystem.BlockSystem;
import net.starborne.server.blocksystem.BlockSystemServer;
import net.starborne.server.blocksystem.BlockSystemTrackingHandler;
import net.starborne.server.blocksystem.ServerBlockSystemHandler;
import net.starborne.server.world.data.BlockSystemSavedData;

import java.util.Map;

public class ServerEventHandler {
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        ServerBlockSystemHandler handler = Starborne.PROXY.getBlockSystemHandler(world);
        if (handler != null) {
            handler.update();
        }
        if (world instanceof WorldServer) {
            BlockSystemTrackingHandler trackingHandler = BlockSystemTrackingHandler.get((WorldServer) world);
            trackingHandler.update();
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        World world = event.getWorld();
        Entity entity = event.getEntity();
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            ServerBlockSystemHandler handler = Starborne.PROXY.getBlockSystemHandler(world);
            Map<Integer, BlockSystem> blockSystems = handler.getBlockSystems();
            handler.addPlayer(player);
            if (world instanceof WorldServer && player instanceof EntityPlayerMP) {
                for (Map.Entry<Integer, BlockSystem> entry : blockSystems.entrySet()) {
                    BlockSystem blockSystem = entry.getValue();
                    if (blockSystem instanceof BlockSystemServer) {
                        ((BlockSystemServer) blockSystem).getChunkTracker().addPlayer((EntityPlayerMP) player);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        World world = entity.worldObj;
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (world instanceof WorldServer && player instanceof EntityPlayerMP) {
                Map<Integer, BlockSystem> blockSystems = Starborne.PROXY.getBlockSystemHandler(world).getBlockSystems();
                for (Map.Entry<Integer, BlockSystem> entry : blockSystems.entrySet()) {
                    BlockSystem blockSystem = entry.getValue();
                    if (blockSystem instanceof BlockSystemServer) {
                        ((BlockSystemServer) blockSystem).getChunkTracker().removePlayer((EntityPlayerMP) player);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        EnumHand hand = event.getHand();
        ServerBlockSystemHandler structureHandler = Starborne.PROXY.getBlockSystemHandler(event.getWorld());
        if (structureHandler.onItemRightClick(player, hand)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        BlockSystemSavedData.get(world);
        if (world instanceof WorldServer) {
            BlockSystemTrackingHandler.add((WorldServer) world);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        BlockSystemSavedData.get(world);
        if (world instanceof WorldServer) {
            BlockSystemTrackingHandler.remove((WorldServer) world);
        }
    }
}
