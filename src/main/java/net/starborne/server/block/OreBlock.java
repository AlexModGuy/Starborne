package net.starborne.server.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.starborne.server.tab.TabRegistry;

public class OreBlock extends Block {
    public OreBlock(float hardness, int harvestLevel) {
        super(Material.ROCK);
        this.setHardness(hardness);
        this.setHarvestLevel("pickaxe", harvestLevel);
        this.setCreativeTab(TabRegistry.BLOCKS);
    }
}
