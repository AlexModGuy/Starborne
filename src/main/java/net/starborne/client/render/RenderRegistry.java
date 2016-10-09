package net.starborne.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.starborne.Starborne;
import net.starborne.client.render.entity.BlockSystemControlRenderer;
import net.starborne.server.api.DefaultRenderedItem;
import net.starborne.server.block.BlockRegistry;
import net.starborne.server.entity.BlockSystemControlEntity;
import net.starborne.server.item.ItemRegistry;

public class RenderRegistry {
    private static final Minecraft MC = Minecraft.getMinecraft();

    public static void onPreInit() {
        RenderingRegistry.registerEntityRenderingHandler(BlockSystemControlEntity.class, BlockSystemControlRenderer::new);
        for (Item item : ItemRegistry.ITEMS) {
            if (item instanceof DefaultRenderedItem) {
                RenderRegistry.registerRenderer(item, ((DefaultRenderedItem) item).getResource(item.getUnlocalizedName().substring("item.".length())));
            }
        }
        for (Block block : BlockRegistry.BLOCKS) {
            if (block instanceof DefaultRenderedItem) {
                RenderRegistry.registerRenderer(block, ((DefaultRenderedItem) block).getResource(block.getUnlocalizedName().substring("tile.".length())));
            }
        }
    }

    public static void onInit() {
    }

    public static void onPostInit() {
    }

    private static void registerRenderer(Item item, String name) {
        ModelResourceLocation resource = new ModelResourceLocation(Starborne.MODID + ":" + name, "inventory");
        ModelLoader.setCustomModelResourceLocation(item, 0, resource);
    }

    private static void registerRenderer(Block block, String name) {
        registerRenderer(Item.getItemFromBlock(block), name);
    }
}
