package net.starborne.server.world.provider;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.starborne.server.dimension.DimensionHandler;
import net.starborne.server.dimension.PlanetDimension;

public class PlanetWorldProvider extends WorldProvider {
    private PlanetDimension dimension;

    @Override
    public void setDimension(int id) {
        super.setDimension(id);
        this.dimension = DimensionHandler.get(id);
    }

    @Override
    public DimensionType getDimensionType() {
        return this.dimension.getDimensionType();
    }
}
