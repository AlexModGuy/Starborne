package net.starborne.server.item;

import net.minecraft.item.Item;
import net.starborne.server.api.DefaultRenderedItem;
import net.starborne.server.tab.TabRegistry;

public class IngotItem extends Item implements DefaultRenderedItem {
    public IngotItem() {
        super();
        this.setCreativeTab(TabRegistry.ITEMS);
    }
}
