package net.starborne.server.space;

import net.starborne.server.dimension.DimensionHandler;
import net.starborne.server.dimension.PlanetDimension;
import net.starborne.server.world.provider.PlanetWorldProvider;

public class Planet extends CelestialObject<Star> implements DimensionalObject {
    @Override
    public PlanetDimension createDimension() {
        return DimensionHandler.createDimension(PlanetWorldProvider.class, this);
    }
}
