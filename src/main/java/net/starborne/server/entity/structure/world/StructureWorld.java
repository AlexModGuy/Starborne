package net.starborne.server.entity.structure.world;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.starborne.server.entity.structure.StructureEntity;
import org.apache.logging.log4j.LogManager;

import javax.vecmath.Vector3d;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class StructureWorld extends World {
    protected StructureEntity entity;
    protected World fallback;

    protected final Set<NextTickListEntry> scheduledTicksSet = Sets.newHashSet();
    protected final TreeSet<NextTickListEntry> scheduledTicksTree = new TreeSet<>();
    protected final List<NextTickListEntry> currentScheduledTicks = new ArrayList<>();

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

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return this.entity.getTileEntity(pos);
    }

    @Override
    public Entity getEntityByID(int id) {
        return this.fallback.getEntityByID(id);
    }

    @Override
    public EntityPlayer getClosestPlayer(double posX, double posY, double posZ, double distance, boolean spectator) {
        Vector3d transformed = this.entity.getTransformedPosition(new Vector3d(posX, posY, posZ));
        posX = transformed.getX();
        posY = transformed.getY();
        posZ = transformed.getZ();
        return this.fallback.getClosestPlayer(posX, posY, posZ, distance, spectator);
    }

    @Override
    public EntityPlayer getNearestAttackablePlayer(double posX, double posY, double posZ, double maxXZDistance, double maxYDistance, Function<EntityPlayer, Double> serializer, Predicate<EntityPlayer> selector) {
        Vector3d transformed = this.entity.getTransformedPosition(new Vector3d(posX, posY, posZ));
        posX = transformed.getX();
        posY = transformed.getY();
        posZ = transformed.getZ();
        return this.fallback.getNearestAttackablePlayer(posX, posY, posZ, maxXZDistance, maxYDistance, serializer, selector);
    }

    @Override
    public List<Entity> getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB bounds) {//TODO rotatable & transformed bounds
        return this.fallback.getEntitiesWithinAABBExcludingEntity(entity, bounds);
    }

    @Override
    public List<Entity> getEntitiesInAABBexcluding(Entity entity, AxisAlignedBB bounds, Predicate<? super Entity> selector) {
        return this.fallback.getEntitiesInAABBexcluding(entity, bounds, selector);
    }

    @Override
    public List<Entity> getLoadedEntityList() {
        return this.fallback.getLoadedEntityList();
    }

    @Override
    public EntityPlayer getPlayerEntityByUUID(UUID uuid) {
        return this.fallback.getPlayerEntityByUUID(uuid);
    }

    @Override
    public EntityPlayer getPlayerEntityByName(String name) {
        return this.fallback.getPlayerEntityByName(name);
    }

    @Override
    public boolean spawnEntityInWorld(Entity entity) {
        Vector3d transformedPosition = this.entity.getTransformedPosition(new Vector3d(entity.posX, entity.posY, entity.posZ));
        entity.posX = transformedPosition.getX();
        entity.posY = transformedPosition.getY();
        entity.posZ = transformedPosition.getZ();
        return this.fallback.spawnEntityInWorld(entity);
    }

    @Override
    public void spawnParticle(EnumParticleTypes particleType, double posX, double posY, double posZ, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        Vector3d transformed = this.entity.getTransformedPosition(new Vector3d(posX, posY, posZ));
        posX = transformed.getX();
        posY = transformed.getY();
        posZ = transformed.getZ();
        super.spawnParticle(particleType, posX, posY, posZ, xSpeed, ySpeed, zSpeed, parameters);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void spawnParticle(EnumParticleTypes particleType, boolean ignoreRange, double posX, double posY, double posZ, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        Vector3d transformed = this.entity.getTransformedPosition(new Vector3d(posX, posY, posZ));
        posX = transformed.getX();
        posY = transformed.getY();
        posZ = transformed.getZ();
        super.spawnParticle(particleType, ignoreRange, posX, posY, posZ, xSpeed, ySpeed, zSpeed, parameters);
    }

    @Override
    public void updateBlockTick(BlockPos pos, Block block, int delay, int priority) {
        if (pos instanceof BlockPos.MutableBlockPos) {
            pos = new BlockPos(pos);
            LogManager.getLogger().warn("Tried to assign a mutable BlockPos to tick data...", new Error(pos.getClass().toString()));
        }
        Material material = block.getDefaultState().getMaterial();
        if (this.scheduledUpdatesAreImmediate && material != Material.AIR) {
            if (block.requiresUpdates()) {
                boolean isForced = this.getPersistentChunks().containsKey(new ChunkPos(pos));
                int range = isForced ? 0 : 8;
                if (this.isAreaLoaded(pos.add(-range, -range, -range), pos.add(range, range, range))) {
                    IBlockState state = this.getBlockState(pos);
                    if (state.getMaterial() != Material.AIR && state.getBlock() == block) {
                        state.getBlock().updateTick(this, pos, state, this.rand);
                    }
                }
                return;
            }
            delay = 1;
        }
        NextTickListEntry schedule = new NextTickListEntry(pos, block);
        if (this.isBlockLoaded(pos)) {
            if (material != Material.AIR) {
                schedule.setScheduledTime((long) delay + this.worldInfo.getWorldTotalTime());
                schedule.setPriority(priority);
            }
            if (!this.scheduledTicksSet.contains(schedule)) {
                this.scheduledTicksSet.add(schedule);
                this.scheduledTicksTree.add(schedule);
            }
        }
    }

    @Override
    public void scheduleBlockUpdate(BlockPos pos, Block block, int delay, int priority) {
        NextTickListEntry schedule = new NextTickListEntry(pos, block);
        schedule.setPriority(priority);
        Material material = block.getDefaultState().getMaterial();
        if (material != Material.AIR) {
            schedule.setScheduledTime((long) delay + this.worldInfo.getWorldTotalTime());
        }
        if (!this.scheduledTicksSet.contains(schedule)) {
            this.scheduledTicksSet.add(schedule);
            this.scheduledTicksTree.add(schedule);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.tickUpdates(false);
    }

    @Override
    public boolean tickUpdates(boolean checkTime) {
        int updates = this.scheduledTicksTree.size();
        if (updates != this.scheduledTicksSet.size()) {
            throw new IllegalStateException("TickNextTick list out of sync");
        } else {
            if (updates > 65536) {
                updates = 65536;
            }
            this.theProfiler.startSection("cleaning");
            for (int i = 0; i < updates; i++) {
                NextTickListEntry scheduledTick = this.scheduledTicksTree.first();
                if (!checkTime && scheduledTick.scheduledTime > this.worldInfo.getWorldTotalTime()) {
                    break;
                }
                this.scheduledTicksTree.remove(scheduledTick);
                this.scheduledTicksSet.remove(scheduledTick);
                this.currentScheduledTicks.add(scheduledTick);
            }
            this.theProfiler.endSection();
            this.theProfiler.startSection("ticking");
            for (NextTickListEntry scheduledTick : this.currentScheduledTicks) {
                if (this.isAreaLoaded(scheduledTick.position.add(0, 0, 0), scheduledTick.position.add(0, 0, 0))) {
                    IBlockState state = this.getBlockState(scheduledTick.position);
                    if (state.getMaterial() != Material.AIR && Block.isEqualTo(state.getBlock(), scheduledTick.getBlock())) {
                        try {
                            state.getBlock().updateTick(this, scheduledTick.position, state, this.rand);
                        } catch (Throwable throwable) {
                            CrashReport report = CrashReport.makeCrashReport(throwable, "Exception while ticking a block");
                            CrashReportCategory category = report.makeCategory("Block being ticked");
                            CrashReportCategory.addBlockInfo(category, scheduledTick.position, state);
                            throw new ReportedException(report);
                        }
                    }
                } else {
                    this.scheduleUpdate(scheduledTick.position, scheduledTick.getBlock(), 0);
                }
            }
            this.currentScheduledTicks.clear();
            this.theProfiler.endSection();
            return !this.scheduledTicksTree.isEmpty();
        }
    }

    @Override
    public List<NextTickListEntry> getPendingBlockUpdates(StructureBoundingBox bounds, boolean remove) {
        List<NextTickListEntry> updates = null;
        for (int i = 0; i < 2; i++) {
            Iterator<NextTickListEntry> iterator;
            if (i == 0) {
                iterator = this.scheduledTicksTree.iterator();
            } else {
                iterator = this.currentScheduledTicks.iterator();
            }
            while (iterator.hasNext()) {
                NextTickListEntry scheduledUpdate = iterator.next();
                BlockPos position = scheduledUpdate.position;
                if (position.getX() >= bounds.minX && position.getX() < bounds.maxX && position.getZ() >= bounds.minZ && position.getZ() < bounds.maxZ) {
                    if (remove) {
                        if (i == 0) {
                            this.scheduledTicksSet.remove(scheduledUpdate);
                        }
                        iterator.remove();
                    }
                    if (updates == null) {
                        updates = Lists.newArrayList();
                    }
                    updates.add(scheduledUpdate);
                }
            }
        }
        return updates;
    }

    @Override
    public boolean isBlockTickPending(BlockPos pos, Block block) {
        NextTickListEntry scheduledTick = new NextTickListEntry(pos, block);
        return this.currentScheduledTicks.contains(scheduledTick);
    }

    @Override
    protected void updateWeather() {
    }
}
