package net.starborne.server.dimension;

import net.minecraft.world.DimensionType;
import net.starborne.server.space.DimensionalObject;

public class PlanetDimension {
    private final int id;
    private final DimensionType dimensionType;
    private final DimensionalObject dimensionalObject;

    public PlanetDimension(int id, DimensionType dimensionType) {
        this(id, dimensionType, null);
    }

    public PlanetDimension(int id, DimensionType dimensionType, DimensionalObject dimensionalObject) {
        this.id = id;
        this.dimensionType = dimensionType;
        this.dimensionalObject = dimensionalObject;
    }

    public int getID() {
        return this.id;
    }

    public DimensionType getDimensionType() {
        return this.dimensionType;
    }

    public DimensionalObject getDimensionalObject() {
        return this.dimensionalObject;
    }
}
