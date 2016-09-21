package net.starborne.server.world.provider;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkGenerator;
import net.starborne.server.world.generator.SpaceChunkGenerator;

public class SpaceWorldProvider extends WorldProvider {
    @Override
    public DimensionType getDimensionType() {
        return DimensionType.getById(this.getDimension());
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }

    @Override
    public boolean isSurfaceWorld()
    {
        return false;
    }

    @Override
    public boolean isSkyColored() {
        return false;
    }

    @Override
    public float getStarBrightness(float f) {
        return 1.0F;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new SpaceChunkGenerator(this.worldObj);
    }
}
