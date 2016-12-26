package net.starborne.server.recipe;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.starborne.server.block.BlockRegistry;
import net.starborne.server.item.ItemRegistry;

public class RecipeRegistry {
    public static void onPreInit() {
        OreDictionary.registerOre("ingotCopper", ItemRegistry.COPPER_INGOT);
        OreDictionary.registerOre("ingotTin", ItemRegistry.TIN_INGOT);
        OreDictionary.registerOre("ingotLithium", ItemRegistry.LITHIUM);
        OreDictionary.registerOre("ingotAluminum", ItemRegistry.ALUMINIUM_INGOT);
        OreDictionary.registerOre("ingotAluminium", ItemRegistry.ALUMINIUM_INGOT);
        OreDictionary.registerOre("ingotSteel", ItemRegistry.STEEL_INGOT);
        OreDictionary.registerOre("oreCopper", BlockRegistry.COPPER_ORE);
        OreDictionary.registerOre("oreTin", BlockRegistry.TIN_ORE);
        OreDictionary.registerOre("oreLithium", BlockRegistry.PETALITE_ORE);
        OreDictionary.registerOre("oreAluminum", BlockRegistry.ALUMINIUM_ORE);
        OreDictionary.registerOre("oreAluminium", BlockRegistry.ALUMINIUM_ORE);

        GameRegistry.addSmelting(BlockRegistry.COPPER_ORE, new ItemStack(ItemRegistry.COPPER_INGOT), 1);
        GameRegistry.addSmelting(BlockRegistry.TIN_ORE, new ItemStack(ItemRegistry.TIN_INGOT), 1);
        GameRegistry.addSmelting(BlockRegistry.ALUMINIUM_ORE, new ItemStack(ItemRegistry.ALUMINIUM_INGOT), 1);
        GameRegistry.addSmelting(BlockRegistry.PETALITE_ORE, new ItemStack(ItemRegistry.LITHIUM), 1);
        GameRegistry.addSmelting(ItemRegistry.IRON_COAL_BALL, new ItemStack(ItemRegistry.STEEL_INGOT), 1);
        GameRegistry.addShapelessRecipe(new ItemStack(ItemRegistry.IRON_COAL_BALL, 1, 0), Items.COAL, Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT);

    }
}