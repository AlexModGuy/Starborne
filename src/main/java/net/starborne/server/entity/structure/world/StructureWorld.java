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
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ReportedException;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.starborne.server.biome.BiomeHandler;
import net.starborne.server.entity.structure.ClientEntityChunk;
import net.starborne.server.entity.structure.EntityChunk;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.StructurePlayerHandler;
import org.apache.logging.log4j.LogManager;

import javax.vecmath.Point3d;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class StructureWorld extends World {
    public static StructureWorld transforming;

    protected StructureEntity entity;
    protected World fallback;

    protected final Set<NextTickListEntry> scheduledTicksSet = Sets.newHashSet();
    protected final TreeSet<NextTickListEntry> scheduledTicksTree = new TreeSet<>();
    protected final List<NextTickListEntry> currentScheduledTicks = new ArrayList<>();

    protected Map<BlockPos, EntityChunk> chunks = new HashMap<>();
    private ArrayDeque<ChunkQueue> queuedChunks = new ArrayDeque<>();

    private int skylightSubtracted;

    public StructureWorld(StructureEntity entity) {
        super(new EntityWorldSaveHandler(), entity.worldObj.getWorldInfo(), entity.worldObj.provider, entity.worldObj.theProfiler, entity.worldObj.isRemote);
        this.entity = entity;
        this.fallback = entity.worldObj;
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        return this.fallback.getChunkProvider();
    }

    @Override
    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
        return false;
    }

    @Override
    public boolean setBlockState(BlockPos pos, IBlockState newState, int flags) {
        return this.setBlockState(pos, newState);
    }

    @Override
    public void markAndNotifyBlock(BlockPos pos, Chunk chunk, IBlockState oldState, IBlockState newState, int flags) {
        if ((flags & 2) != 0 && (!this.isRemote || (flags & 4) == 0) && this.getChunkForBlock(pos) == null) {
            this.notifyBlockUpdate(pos, oldState, newState, flags);
        }
        if (!this.isRemote && (flags & 1) != 0) {
            this.notifyNeighborsRespectDebug(pos, oldState.getBlock());
            if (newState.hasComparatorInputOverride()) {
                this.updateComparatorOutputLevel(pos, newState.getBlock());
            }
        }
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        EntityChunk chunk = this.getChunk(this.getChunkPosition(pos));
        if (chunk != null) {
            return chunk.getBlockState(this.getPositionInChunk(pos));
        }
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public int getLight(BlockPos pos) {
        return 15;
    }

    @Override
    public int getLightFromNeighbors(BlockPos pos) {
        return this.getLight(pos, true);
    }

    @Override
    public int getLight(BlockPos pos, boolean checkNeighbors) {
        return 15;
    }

    @Override
    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        return 15;
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return BiomeHandler.SPACE;
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing side) {
        return this.getBlockState(pos).getStrongPower(this, pos, side);
    }

    @Override
    public WorldType getWorldType() {
        return WorldType.DEFAULT;
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        IBlockState state = this.getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        EntityChunk chunk = this.getChunkForBlock(pos);
        if (chunk != null && !chunk.isEmpty()) {
            return chunk.getTileEntity(this.getPositionInChunk(pos));
        }
        return null;
    }

    @Override
    public Entity getEntityByID(int id) {
        return this.fallback.getEntityByID(id);
    }

    @Override
    public EntityPlayer getClosestPlayer(double posX, double posY, double posZ, double distance, boolean spectator) {
        Point3d transformed = this.entity.getTransformedPosition(new Point3d(posX, posY, posZ));
        posX = transformed.getX();
        posY = transformed.getY();
        posZ = transformed.getZ();
        return this.fallback.getClosestPlayer(posX, posY, posZ, distance, spectator);
    }

    @Override
    public EntityPlayer getNearestAttackablePlayer(double posX, double posY, double posZ, double maxXZDistance, double maxYDistance, Function<EntityPlayer, Double> serializer, Predicate<EntityPlayer> selector) {
        Point3d transformed = this.entity.getTransformedPosition(new Point3d(posX, posY, posZ));
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
        entity.worldObj = this.fallback;
        Point3d transformedPosition = this.entity.getTransformedPosition(new Point3d(entity.posX, entity.posY, entity.posZ));
        entity.posX = transformedPosition.getX();
        entity.posY = transformedPosition.getY();
        entity.posZ = transformedPosition.getZ();
        return this.fallback.spawnEntityInWorld(entity);
    }

    @Override
    public void spawnParticle(EnumParticleTypes particleType, double posX, double posY, double posZ, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        Point3d transformed = this.entity.getTransformedPosition(new Point3d(posX, posY, posZ));
        posX = transformed.getX();
        posY = transformed.getY();
        posZ = transformed.getZ();
        Vec3d transformedVelocity = this.entity.getTransformedVector(new Vec3d(xSpeed, ySpeed, zSpeed));
        xSpeed = transformedVelocity.xCoord;
        ySpeed = transformedVelocity.yCoord;
        zSpeed = transformedVelocity.zCoord;
        super.spawnParticle(particleType, posX, posY, posZ, xSpeed, ySpeed, zSpeed, parameters);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void spawnParticle(EnumParticleTypes particleType, boolean ignoreRange, double posX, double posY, double posZ, double xSpeed, double ySpeed, double zSpeed, int... parameters) {
        Point3d transformed = this.entity.getTransformedPosition(new Point3d(posX, posY, posZ));
        posX = transformed.getX();
        posY = transformed.getY();
        posZ = transformed.getZ();
        Vec3d transformedVelocity = this.entity.getTransformedVector(new Vec3d(xSpeed, ySpeed, zSpeed));
        xSpeed = transformedVelocity.xCoord;
        ySpeed = transformedVelocity.yCoord;
        zSpeed = transformedVelocity.zCoord;
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
        this.updateChunkQueue();
        //TODO only update tracked chunks?
        for (Map.Entry<BlockPos, EntityChunk> entry : this.chunks.entrySet()) {
            entry.getValue().update();
        }
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

    @Override
    public RayTraceResult rayTraceBlocks(Vec3d start, Vec3d end, boolean traceLiquid, boolean ignoreBlockWithoutBoundingBox, boolean returnLastUncollidableBlock) {
        if (!Double.isNaN(start.xCoord) && !Double.isNaN(start.yCoord) && !Double.isNaN(start.zCoord)) {
            if (!Double.isNaN(end.xCoord) && !Double.isNaN(end.yCoord) && !Double.isNaN(end.zCoord)) {
                start = this.entity.getUntransformedPosition(start);
                end = this.entity.getUntransformedPosition(end);
                int endX = MathHelper.floor_double(end.xCoord);
                int endY = MathHelper.floor_double(end.yCoord);
                int endZ = MathHelper.floor_double(end.zCoord);
                int traceX = MathHelper.floor_double(start.xCoord);
                int traceY = MathHelper.floor_double(start.yCoord);
                int traceZ = MathHelper.floor_double(start.zCoord);
                BlockPos tracePos = new BlockPos(traceX, traceY, traceZ);
                IBlockState startState = this.getBlockState(tracePos);
                Block startBlock = startState.getBlock();
                if ((!ignoreBlockWithoutBoundingBox || startState.getCollisionBoundingBox(this, tracePos) != Block.NULL_AABB) && startBlock.canCollideCheck(startState, traceLiquid)) {
                    RayTraceResult result = startState.collisionRayTrace(this, tracePos, start, end);
                    if (result != null) {
                        return result;
                    }
                }
                RayTraceResult result = null;
                int ray = 200;
                while (ray-- >= 0) {
                    if (Double.isNaN(start.xCoord) || Double.isNaN(start.yCoord) || Double.isNaN(start.zCoord)) {
                        return null;
                    }
                    if (traceX == endX && traceY == endY && traceZ == endZ) {
                        return returnLastUncollidableBlock ? result : null;
                    }
                    boolean reachedX = true;
                    boolean reachedY = true;
                    boolean reachedZ = true;
                    double targetX = 999.0;
                    double targetY = 999.0;
                    double targetZ = 999.0;
                    if (endX > traceX) {
                        targetX = traceX + 1.0;
                    } else if (endX < traceX) {
                        targetX = traceX;
                    } else {
                        reachedX = false;
                    }
                    if (endY > traceY) {
                        targetY = traceY + 1.0;
                    } else if (endY < traceY) {
                        targetY = traceY;
                    } else {
                        reachedY = false;
                    }
                    if (endZ > traceZ) {
                        targetZ = traceZ + 1.0;
                    } else if (endZ < traceZ) {
                        targetZ = traceZ;
                    } else {
                        reachedZ = false;
                    }
                    double deltaX = 999.0;
                    double deltaY = 999.0;
                    double deltaZ = 999.0;
                    double totalDeltaX = end.xCoord - start.xCoord;
                    double totalDeltaY = end.yCoord - start.yCoord;
                    double totalDeltaZ = end.zCoord - start.zCoord;
                    if (reachedX) {
                        deltaX = (targetX - start.xCoord) / totalDeltaX;
                    }
                    if (reachedY) {
                        deltaY = (targetY - start.yCoord) / totalDeltaY;
                    }
                    if (reachedZ) {
                        deltaZ = (targetZ - start.zCoord) / totalDeltaZ;
                    }
                    if (deltaX == -0.0) {
                        deltaX = -1.0E-4D;
                    }
                    if (deltaY == -0.0) {
                        deltaY = -1.0E-4D;
                    }
                    if (deltaZ == -0.0) {
                        deltaZ = -1.0E-4D;
                    }
                    EnumFacing sideHit;
                    if (deltaX < deltaY && deltaX < deltaZ) {
                        sideHit = endX > traceX ? EnumFacing.WEST : EnumFacing.EAST;
                        start = new Vec3d(targetX, start.yCoord + totalDeltaY * deltaX, start.zCoord + totalDeltaZ * deltaX);
                    } else if (deltaY < deltaZ) {
                        sideHit = endY > traceY ? EnumFacing.DOWN : EnumFacing.UP;
                        start = new Vec3d(start.xCoord + totalDeltaX * deltaY, targetY, start.zCoord + totalDeltaZ * deltaY);
                    } else {
                        sideHit = endZ > traceZ ? EnumFacing.NORTH : EnumFacing.SOUTH;
                        start = new Vec3d(start.xCoord + totalDeltaX * deltaZ, start.yCoord + totalDeltaY * deltaZ, targetZ);
                    }
                    traceX = MathHelper.floor_double(start.xCoord) - (sideHit == EnumFacing.EAST ? 1 : 0);
                    traceY = MathHelper.floor_double(start.yCoord) - (sideHit == EnumFacing.UP ? 1 : 0);
                    traceZ = MathHelper.floor_double(start.zCoord) - (sideHit == EnumFacing.SOUTH ? 1 : 0);
                    tracePos = new BlockPos(traceX, traceY, traceZ);
                    IBlockState traceState = this.getBlockState(tracePos);
                    Block traceBlock = traceState.getBlock();
                    if (!ignoreBlockWithoutBoundingBox || traceState.getMaterial() == Material.PORTAL || traceState.getCollisionBoundingBox(this, tracePos) != Block.NULL_AABB) {
                        if (traceBlock.canCollideCheck(traceState, traceLiquid)) {
                            RayTraceResult finalResult = traceState.collisionRayTrace(this, tracePos, start, end);
                            if (finalResult != null) {
                                return finalResult;
                            }
                        } else {
                            result = new RayTraceResult(RayTraceResult.Type.MISS, start, sideHit, tracePos);
                        }
                    }
                }
                return returnLastUncollidableBlock ? result : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void playEventServer(EntityPlayer player, int type, BlockPos pos, int data) {
        super.playEvent(player, type, pos, data);
    }

    @Override
    public void playEvent(EntityPlayer player, int type, BlockPos pos, int data) {
        StructureWorld.transforming = this;
        super.playEvent(player, type, pos, data);
        StructureWorld.transforming = null;
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        EntityChunk chunk = this.getChunkForBlock(pos);
        if (chunk == null || chunk.isEmpty()) {
            return _default;
        }
        return this.getBlockState(pos).isSideSolid(this, pos, side);
    }

    @Override
    public boolean setBlockState(BlockPos pos, IBlockState state) {
        BlockPos chunkPosition = this.getChunkPosition(pos);
        EntityChunk chunk = this.chunks.get(chunkPosition);
        if (chunk == null) {
            for (StructureWorld.ChunkQueue queue : this.queuedChunks) {
                if (!queue.remove && queue.position.equals(chunkPosition)) {
                    chunk = queue.chunk;
                }
            }
        }
        if (chunk == null) {
            if (state.getBlock() == Blocks.AIR) {
                return false;
            }
            chunk = this.isRemote ? new ClientEntityChunk(this.entity, chunkPosition) : new EntityChunk(this.entity, chunkPosition);
            this.setChunk(chunkPosition, chunk);
        }
        boolean success = chunk.setBlockState(this.getPositionInChunk(pos), state);
        if (chunk.isEmpty()) {
            chunk.unload();
            this.queuedChunks.add(new StructureWorld.ChunkQueue(chunkPosition, chunk, true));
        }
        for (Map.Entry<EntityPlayer, StructurePlayerHandler> entry : this.entity.getPlayerHandlers().entrySet()) {
            entry.getValue().setDirty(chunk);
        }
        return success;
    }

    @Override
    public void playSound(EntityPlayer player, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        Point3d transformed = this.entity.getTransformedPosition(new Point3d(x, y, z));
        x = transformed.getX();
        y = transformed.getY();
        z = transformed.getZ();
        super.playSound(player, x, y, z, sound, category, volume, pitch);
    }

    public BlockPos getChunkPosition(BlockPos pos) {
        return new BlockPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    public BlockPos getPositionInChunk(BlockPos pos) {
        return new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }

    public EntityChunk getChunkForBlock(BlockPos pos) {
        return this.chunks.get(this.getChunkPosition(pos));
    }

    public EntityChunk getChunk(BlockPos pos) {
        return this.chunks.get(pos);
    }

    public Map<BlockPos, EntityChunk> getChunks() {
        return this.chunks;
    }

    public List<IWorldEventListener> getListeners() {
        return this.eventListeners;
    }

    public StructureEntity getEntity() {
        return this.entity;
    }

    public World getMainWorld() {
        return this.fallback;
    }

    public void updateChunkQueue() {
        while (this.queuedChunks.size() > 0) {
            StructureWorld.ChunkQueue queue = this.queuedChunks.poll();
            if (queue.remove) {
                this.chunks.remove(queue.position);
            } else {
                this.chunks.put(queue.position, queue.chunk);
            }
            if (this.chunks.size() <= 0) {
                this.entity.setDead();
            }
        }
    }

    public void setChunk(BlockPos position, EntityChunk chunk) {
        EntityChunk previous = this.getChunks().get(position);
        this.queuedChunks.add(new ChunkQueue(position, chunk, false));
        if (previous != null) {
            previous.unload();
            for (Map.Entry<EntityPlayer, StructurePlayerHandler> entry : this.entity.getPlayerHandlers().entrySet()) {
                entry.getValue().remove(previous);
            }
        }
        this.entity.recalculateCenter();
    }

    private class ChunkQueue {
        private boolean remove;
        private BlockPos position;
        private EntityChunk chunk;

        private ChunkQueue(BlockPos position, EntityChunk chunk, boolean remove) {
            this.remove = remove;
            this.position = position;
            this.chunk = chunk;
        }
    }
}
