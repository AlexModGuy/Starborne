package net.starborne.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.starborne.Starborne;
import net.starborne.client.render.blocksystem.BlockSystemRenderHandler;
import net.starborne.server.blocksystem.ServerBlockSystemHandler;
import net.starborne.server.blocksystem.BlockSystem;
import net.starborne.server.blocksystem.BlockSystemPlayerHandler;

public class ClientEventHandler {
    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        WorldClient world = MINECRAFT.theWorld;
        if (world != null) {
            Starborne.PROXY.getBlockSystemHandler(world).update();
            BlockSystemRenderHandler.update();
        }
    }

    @SubscribeEvent
    public void onRightClickAir(PlayerInteractEvent.RightClickEmpty event) {
        EntityPlayer player = event.getEntityPlayer();
        ServerBlockSystemHandler structureHandler = Starborne.PROXY.getBlockSystemHandler(event.getWorld());
        structureHandler.interact(structureHandler.get(structureHandler.getMousedOver(player), player), player, event.getHand());
    }

    @SubscribeEvent
    public void onClickAir(PlayerInteractEvent.LeftClickEmpty event) {
        EntityPlayer player = event.getEntityPlayer();
        BlockSystem mousedOver = Starborne.PROXY.getBlockSystemHandler(event.getWorld()).getMousedOver(player);
        if (mousedOver != null) {
            BlockSystemPlayerHandler mouseOverHandler = Starborne.PROXY.getBlockSystemHandler(event.getWorld()).get(mousedOver, player);
            if (mouseOverHandler != null) {
                RayTraceResult mouseOver = mouseOverHandler.getMouseOver();
                if (mouseOver != null && mouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
                    mouseOverHandler.clickBlock(mouseOver.getBlockPos());
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        World world = event.getWorld();
        ServerBlockSystemHandler structureHandler = Starborne.PROXY.getBlockSystemHandler(world);
        if (entity instanceof EntityPlayer) {
            structureHandler.addPlayer((EntityPlayer) entity);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof EntityPlayer) {
            Starborne.PROXY.getBlockSystemHandler(entity.worldObj).removePlayer((EntityPlayer) entity);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        Starborne.PROXY.getBlockSystemHandler(event.getWorld()).unloadWorld();
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        EntityPlayer player = MINECRAFT.thePlayer;
        float partialTicks = event.getPartialTicks();
        double playerX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double playerY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double playerZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
        BlockSystemRenderHandler.render(player, playerX, playerY, playerZ, partialTicks);
    }
}
