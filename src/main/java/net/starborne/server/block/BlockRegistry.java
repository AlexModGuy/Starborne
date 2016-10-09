package net.starborne.server.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.starborne.Starborne;
import net.starborne.server.util.HarvestLevels;

import java.util.ArrayList;
import java.util.List;

public class BlockRegistry {
    public static final OreBlock COPPER_ORE = new OreBlock(2.0F, HarvestLevels.STONE);
    public static final OreBlock TIN_ORE = new OreBlock(2.0F, HarvestLevels.STONE);
    public static final OreBlock ALUMINIUM_ORE = new OreBlock(2.0F, HarvestLevels.STONE);
    public static final OreBlock PETALITE_ORE = new OreBlock(2.0F, HarvestLevels.STONE);

    public static final List<Block> BLOCKS = new ArrayList<>();

    public static void onPreInit() {
        BlockRegistry.register(COPPER_ORE, new ResourceLocation(Starborne.MODID, "copper_ore"), "oreCopper");
        BlockRegistry.register(TIN_ORE, new ResourceLocation(Starborne.MODID, "tin_ore"), "oreTin");
        BlockRegistry.register(ALUMINIUM_ORE, new ResourceLocation(Starborne.MODID, "aluminium_ore"), "oreAluminium");
        BlockRegistry.register(PETALITE_ORE, new ResourceLocation(Starborne.MODID, "petalite_ore"), "orePetalite");
    }

    private static void register(Block block, ResourceLocation identifier) {
        block.setUnlocalizedName(identifier.getResourcePath());
        GameRegistry.register(block, identifier);
        GameRegistry.register(new ItemBlock(block), identifier);
        BLOCKS.add(block);
    }

    private static void register(Block block, ResourceLocation identifier, String oreDict) {
        BlockRegistry.register(block, identifier);
        OreDictionary.registerOre(oreDict, block);
    }
}
