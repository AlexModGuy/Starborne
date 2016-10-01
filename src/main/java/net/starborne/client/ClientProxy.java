package net.starborne.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.starborne.client.render.RenderRegistry;
import net.starborne.server.ServerProxy;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.world.StructureWorld;
import net.starborne.server.entity.structure.world.StructureWorldClient;

public class ClientProxy extends ServerProxy {
    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();

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

    @Override
    public void playSound(ISound sound) {
        MINECRAFT.getSoundHandler().playSound(sound);
    }

    @Override
    public void pickBlock(EntityPlayer player, RayTraceResult mouseOver, TileEntity tile, World world, IBlockState state) {
        if (player.capabilities.isCreativeMode && GuiScreen.isCtrlKeyDown() && state.getBlock().hasTileEntity(state)) {
            tile = world.getTileEntity(mouseOver.getBlockPos());
        }
        ItemStack result = state.getBlock().getPickBlock(state, mouseOver, world, mouseOver.getBlockPos(), player);
        if (tile != null) {
            MINECRAFT.storeTEInStack(result, tile);
        }
        if (player.capabilities.isCreativeMode) {
            player.inventory.setPickedItemStack(result);
            MINECRAFT.playerController.sendSlotPacket(player.getHeldItem(EnumHand.MAIN_HAND), player.inventory.currentItem + 36);
        } else {
            int slot = player.inventory.getSlotFor(result);
            if (slot != -1) {
                if (InventoryPlayer.isHotbar(slot)) {
                    player.inventory.currentItem = slot;
                } else {
                    MINECRAFT.playerController.pickItem(slot);
                }
            }
        }
    }
}
