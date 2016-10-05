package net.starborne.server.world.generator;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkGenerator;
import net.starborne.server.biome.BiomeHandler;

import javax.annotation.Nullable;
import java.util.List;

public class BlankChunkGenerator implements IChunkGenerator {
    private static final byte BIOME = (byte) Biome.getIdForBiome(BiomeHandler.SPACE);

    private World world;

    public BlankChunkGenerator(World world) {
        this.world = world;
    }

    @Override
    public Chunk provideChunk(int x, int z) {
        Chunk chunk = new Chunk(this.world, x, z);
        byte[] biomes = chunk.getBiomeArray();
        for (int i = 0; i < biomes.length; ++i) {
            biomes[i] = BIOME;
        }
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void populate(int x, int z) {
    }

    @Override
    public boolean generateStructures(Chunk chunk, int x, int z) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        return BiomeHandler.SPACE.getSpawnableList(creatureType);
    }

    @Nullable
    @Override
    public BlockPos getStrongholdGen(World world, String structureName, BlockPos position) {
        return null;
    }

    @Override
    public void recreateStructures(Chunk chunk, int x, int z) {
    }
}
