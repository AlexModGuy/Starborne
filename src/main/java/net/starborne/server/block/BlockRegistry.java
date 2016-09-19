package net.starborne.server.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.starborne.Starborne;
import net.starborne.server.util.HarvestLevels;

public class BlockRegistry {
    public static final OreBlock COPPER_ORE = new OreBlock(2.0F, HarvestLevels.WOOD);

    public static void onPreInit() {
        BlockRegistry.register(COPPER_ORE, new ResourceLocation(Starborne.MODID, "copper_ore"), "oreCopper");
    }

    private static void register(Block block, ResourceLocation identifier) {
        block.setUnlocalizedName(identifier.getResourcePath());
        GameRegistry.register(block, identifier);
        GameRegistry.register(new ItemBlock(block), identifier);
    }

    private static void register(Block block, ResourceLocation identifier, String oreDict) {
        BlockRegistry.register(block, identifier);
        OreDictionary.registerOre(oreDict, block);
    }
}
