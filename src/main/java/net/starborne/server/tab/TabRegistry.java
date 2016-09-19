package net.starborne.server.tab;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.starborne.Starborne;
import net.starborne.server.block.BlockRegistry;
import net.starborne.server.item.ItemRegistry;

public class TabRegistry {
    public static CreativeTabs BLOCKS = new CreativeTabs(Starborne.MODID.toLowerCase() + ".blocks") {
        @Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(BlockRegistry.COPPER_ORE);
        }
    };

    public static CreativeTabs ITEMS = new CreativeTabs(Starborne.MODID.toLowerCase() + ".items") {
        @Override
        public Item getTabIconItem() {
            return ItemRegistry.COPPER_INGOT;
        }
    };
}
