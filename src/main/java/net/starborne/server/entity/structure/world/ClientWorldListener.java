package net.starborne.server.entity.structure.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ReportedException;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;

public class ClientWorldListener implements IWorldEventListener {
    private Minecraft mc = Minecraft.getMinecraft();

    @Override
    public void notifyBlockUpdate(World world, BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
    }

    @Override
    public void notifyLightSet(BlockPos pos) {
    }

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
    }

    @Override
    public void playSoundToAllNearExcept(EntityPlayer player, SoundEvent sound, SoundCategory category, double x, double y, double z, float volume, float pitch) {
    }

    @Override
    public void playRecord(SoundEvent sound, BlockPos pos) {
    }

    @Override
    public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        try {
            this.spawnEffectParticle(particleID, ignoreRange, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, parameters);
        } catch (Throwable throwable) {
            CrashReport crash = CrashReport.makeCrashReport(throwable, "Exception while adding particle");
            CrashReportCategory category = crash.makeCategory("Particle being added");
            category.addCrashSection("ID", particleID);
            category.addCrashSection("Parameters", parameters);
            category.setDetail("Position", () -> CrashReportCategory.getCoordinateInfo(xCoord, yCoord, zCoord));
            throw new ReportedException(crash);
        }
    }

    private Particle spawnEffectParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord, double zCoord, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        Entity entity = this.mc.getRenderViewEntity();
        if (this.mc != null && entity != null && this.mc.effectRenderer != null) {
            int particleSetting = this.mc.gameSettings.particleSetting;
            if (particleSetting == 1 && this.mc.theWorld.rand.nextInt(3) == 0) {
                particleSetting = 2;
            }
            double deltaX = entity.posX - xCoord;
            double deltaY = entity.posY - yCoord;
            double deltaZ = entity.posZ - zCoord;
            if (ignoreRange) {
                return this.mc.effectRenderer.spawnEffectParticle(particleID, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, parameters);
            } else {
                if (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 1024.0D) {
                    return null;
                } else {
                    if (particleSetting > 1) {
                        return null;
                    } else {
                        return this.mc.effectRenderer.spawnEffectParticle(particleID, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed, parameters);
                    }
                }
            }
        } else {
            return null;
        }
    }

    @Override
    public void onEntityAdded(Entity entity) {
    }

    @Override
    public void onEntityRemoved(Entity entity) {
    }

    @Override
    public void broadcastSound(int soundID, BlockPos pos, int data) {
    }

    @Override
    public void playEvent(EntityPlayer player, int type, BlockPos pos, int data) {
        this.mc.renderGlobal.playEvent(player, type, pos, data);
    }

    @Override
    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
    }

    public static ClientWorldListener get(StructureWorld world) {
        for (IWorldEventListener listener : world.getListeners()) {
            if (listener instanceof ClientWorldListener) {
                return (ClientWorldListener) listener;
            }
        }
        return null;
    }
}
