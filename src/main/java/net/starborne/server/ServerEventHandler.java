package net.starborne.server;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.starborne.Starborne;

public class ServerEventHandler {
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        Starborne.PROXY.getStructureHandler(event.world).update(event.world);
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        EnumHand hand = event.getHand();
        ServerStructureHandler structureHandler = Starborne.PROXY.getStructureHandler(event.getWorld());
        if (structureHandler.onItemRightClick(player, hand)) {
            event.setCanceled(true);
        }
    }
}
