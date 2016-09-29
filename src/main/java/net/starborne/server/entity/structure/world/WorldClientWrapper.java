package net.starborne.server.entity.structure.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class WorldClientWrapper extends WorldClient {
    private StructureWorld wrap;

    public WorldClientWrapper(StructureWorld world) {
        super(null, new WorldSettings(world.getMainWorld().getWorldInfo()), world.provider.getDimension(), world.getMainWorld().getDifficulty(), world.theProfiler);
        this.wrap = world;
    }

    @Override
    public void playSound(double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean distanceDelay) {
        this.wrap.playSound(x, y, z, sound, category, volume, pitch, distanceDelay);
    }

    @Override
    public void playRecord(BlockPos pos, SoundEvent sound) {
        this.wrap.playRecord(pos, sound);
    }

    @Override
    public void spawnParticle(EnumParticleTypes particleType, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        this.wrap.spawnParticle(particleType, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, parameters);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void spawnParticle(EnumParticleTypes particleType, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        this.wrap.spawnParticle(particleType, ignoreRange, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, parameters);
    }

    @Override
    public boolean setBlockState(BlockPos pos, IBlockState state, int flags) {
        return this.wrap.setBlockState(pos, state, flags);
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return this.wrap.getBlockState(pos);
    }
}
