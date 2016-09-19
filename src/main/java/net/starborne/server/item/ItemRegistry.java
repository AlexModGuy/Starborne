package net.starborne.server.item;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.starborne.Starborne;

public class ItemRegistry {
    public static final IngotItem COPPER_INGOT = new IngotItem();

    public static void onPreInit() {
        ItemRegistry.register(COPPER_INGOT, new ResourceLocation(Starborne.MODID, "copper_ingot"), "ingotCopper");
    }

    private static void register(Item item, ResourceLocation identifier) {
        item.setUnlocalizedName(identifier.getResourcePath());
        GameRegistry.register(item, identifier);
    }

    private static void register(Item item, ResourceLocation identifier, String oreDict) {
        ItemRegistry.register(item, identifier);
        OreDictionary.registerOre(oreDict, item);
    }
}
