package net.starborne.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.starborne.Starborne;
import net.starborne.client.render.entity.StructureEntityRenderer;
import net.starborne.server.block.BlockRegistry;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.item.ItemRegistry;

public class RenderRegistry {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static ItemModelMesher MODEL_MESHER;

    public static void onPreInit() {
        RenderingRegistry.registerEntityRenderingHandler(StructureEntity.class, StructureEntityRenderer::new);
    }

    public static void onInit() {
        MODEL_MESHER = MC.getRenderItem().getItemModelMesher();

        RenderRegistry.registerRenderer(ItemRegistry.COPPER_INGOT);
    }

    public static void onPostInit() {
        RenderRegistry.registerRenderer(BlockRegistry.COPPER_ORE);
    }

    private static void registerRenderer(Item item) {
        MODEL_MESHER.register(item, stack -> new ModelResourceLocation(Starborne.MODID + ":" + item.getUnlocalizedName().substring("item.".length()), "inventory"));
    }

    private static void registerRenderer(Block block) {
        registerRenderer(Item.getItemFromBlock(block));
    }
}
