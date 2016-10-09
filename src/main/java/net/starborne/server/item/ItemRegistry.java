package net.starborne.server.item;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.starborne.Starborne;

import java.util.ArrayList;
import java.util.List;

public class ItemRegistry {
    public static final IngotItem COPPER_INGOT = new IngotItem();
    public static final IngotItem TIN_INGOT = new IngotItem();
    public static final IngotItem ALUMINIUM_INGOT = new IngotItem();
    public static final IngotItem LITHIUM = new IngotItem();
    public static final IngotItem IRON_COAL_BALL = new IngotItem();
    public static final IngotItem STEEL_INGOT = new IngotItem();

    public static final List<Item> ITEMS = new ArrayList<>();

    public static void onPreInit() {
        ItemRegistry.register(COPPER_INGOT, new ResourceLocation(Starborne.MODID, "copper_ingot"), "ingotCopper");
        ItemRegistry.register(TIN_INGOT, new ResourceLocation(Starborne.MODID, "tin_ingot"), "ingotTin");
        ItemRegistry.register(ALUMINIUM_INGOT, new ResourceLocation(Starborne.MODID, "aluminium_ingot"), "ingotAluminum");
        ItemRegistry.register(LITHIUM, new ResourceLocation(Starborne.MODID, "lithium"), "lithium");
        ItemRegistry.register(IRON_COAL_BALL, new ResourceLocation(Starborne.MODID, "iron_coal_ball"), "ironCoalBall");
        ItemRegistry.register(STEEL_INGOT, new ResourceLocation(Starborne.MODID, "steel_ingot"), "ingotSteel");
    }

    private static void register(Item item, ResourceLocation identifier) {
        item.setUnlocalizedName(identifier.getResourcePath());
        GameRegistry.register(item, identifier);
        ITEMS.add(item);
    }

    private static void register(Item item, ResourceLocation identifier, String oreDict) {
        ItemRegistry.register(item, identifier);
        OreDictionary.registerOre(oreDict, item);
    }
}
