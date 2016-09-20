package net.starborne.server.dimension;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.DimensionManager;
import net.starborne.server.space.DimensionalObject;

import java.util.HashMap;
import java.util.Map;

public class DimensionHandler {
    private static final Map<Integer, PlanetDimension> DIMENSIONS = new HashMap<>();

    public static PlanetDimension createDimension(Class<? extends WorldProvider> provider) {
        int id = DimensionManager.getNextFreeDimId();
        DimensionType dimensionType = DimensionType.register("planet_" + id, "_" + id, id, provider, false);
        DimensionManager.registerDimension(id, dimensionType);
        return DIMENSIONS.put(id, new PlanetDimension(id, dimensionType));
    }

    public static PlanetDimension createDimension(Class<? extends WorldProvider> provider, DimensionalObject celestialObject) {
        int id = DimensionManager.getNextFreeDimId();
        DimensionType dimensionType = DimensionType.register("planet_" + id, "_" + id, id, provider, false);
        DimensionManager.registerDimension(id, dimensionType);
        return DIMENSIONS.put(id, new PlanetDimension(id, dimensionType, celestialObject));
    }

    public static PlanetDimension get(int id) {
        return DIMENSIONS.get(id);
    }
}
