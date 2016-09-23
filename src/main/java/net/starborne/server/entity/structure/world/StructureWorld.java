package net.starborne.server.entity.structure.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.starborne.server.entity.structure.StructureEntity;

import java.io.File;

public class StructureWorld extends World {
    private StructureEntity entity;
    private World fallback;

    public StructureWorld(StructureEntity entity) {
        super(new ISaveHandler() {
            @Override
            public WorldInfo loadWorldInfo() {
                return null;
            }

            @Override
            public void checkSessionLock() throws MinecraftException {
            }

            @Override
            public IChunkLoader getChunkLoader(WorldProvider provider) {
                return null;
            }

            @Override
            public void saveWorldInfoWithPlayer(WorldInfo worldInformation, NBTTagCompound tagCompound) {
            }

            @Override
            public void saveWorldInfo(WorldInfo worldInformation) {
            }

            @Override
            public IPlayerFileData getPlayerNBTManager() {
                return null;
            }

            @Override
            public void flush() {
            }

            @Override
            public File getWorldDirectory() {
                return null;
            }

            @Override
            public File getMapFileFromName(String mapName) {
                return null;
            }

            @Override
            public TemplateManager getStructureTemplateManager() {
                return null;
            }
        }, entity.worldObj.getWorldInfo(), entity.worldObj.provider, entity.worldObj.theProfiler, entity.worldObj.isRemote);
        this.entity = entity;
        this.fallback = entity.worldObj;
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        return this.fallback.getChunkProvider();
    }

    @Override
    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
        return true;
    }

    @Override
    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags) {
        return this.entity.setBlockState(pos, newState);
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return this.entity.getBlockState(pos);
    }

    @Override
    public int getLight(BlockPos pos, boolean checkNeighbors) {
        return 15; //TODO Lighting
    }

    @Override
    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        return 15;
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        return this.entity.isAirBlock(pos);
    }
}
