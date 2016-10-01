package net.starborne.server.entity.structure;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.util.Constants;
import net.starborne.Starborne;
import net.starborne.client.ClientEventHandler;
import net.starborne.server.biome.BiomeHandler;
import net.starborne.server.entity.structure.world.StructureWorld;
import net.starborne.server.util.Matrix;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class StructureEntity extends Entity implements IBlockAccess {
    public StructureWorld structureWorld;
    public float rotationRoll;
    public float prevRotationRoll;

    private Map<BlockPos, EntityChunk> chunks;
    private Map<EntityPlayer, StructurePlayerHandler> playerHandlers;

    private ArrayDeque<ChunkQueue> queuedChunks;

    private Matrix transformMatrix = new Matrix(3);
    private Matrix untransformMatrix = new Matrix(3);

    private boolean deserializing;

    public StructureEntity(World world) {
        super(world);
        this.ignoreFrustumCheck = true;
    }

    @Override
    protected void entityInit() {
        if (this.structureWorld == null) {
            this.structureWorld = Starborne.PROXY.createStructureWorld(this);
        }
        if (this.chunks == null) {
            this.chunks = new HashMap<>();
        }
        if (this.playerHandlers == null) {
            this.playerHandlers = new HashMap<>();
        }
        if (this.queuedChunks == null) {
            this.queuedChunks = new ArrayDeque<>();
        }

        if (!this.worldObj.isRemote) {
            this.setBlockState(new BlockPos(0, 0, 0), Blocks.STONE.getDefaultState());

            while (this.queuedChunks.size() > 0) {
                ChunkQueue queue = this.queuedChunks.poll();
                if (queue.remove) {
                    this.chunks.remove(queue.position);
                } else {
                    this.chunks.put(queue.position, queue.chunk);
                }
            }
        }
    }

    @Override
    public void onUpdate() {
        this.prevRotationRoll = this.rotationRoll;
        super.onUpdate();

        this.structureWorld.tick();
        while (this.queuedChunks.size() > 0) {
            ChunkQueue queue = this.queuedChunks.poll();
            if (queue.remove) {
                this.chunks.remove(queue.position);
            } else {
                this.chunks.put(queue.position, queue.chunk);
            }
            if (this.chunks.size() <= 0) {
                this.setDead();
            }
        }
        if (!this.worldObj.isRemote) {
            //TODO Improve trackers further
            for (Map.Entry<EntityPlayer, StructurePlayerHandler> entry : this.playerHandlers.entrySet()) {
                entry.getValue().update();
            }
        }
        //TODO only update tracked chunks?
        for (Map.Entry<BlockPos, EntityChunk> entry : this.chunks.entrySet()) {
            entry.getValue().update();
        }

        this.rotationPitch += 0.1F;
        this.rotationYaw += 0.1F;

        if (this.posX != this.prevPosX || this.posY != this.prevPosY || this.posZ != this.prevPosZ || this.rotationYaw != this.prevRotationYaw || this.rotationPitch != this.prevRotationPitch || this.rotationRoll != this.prevRotationRoll) {
            this.transformMatrix.setIdentity();
            this.transformMatrix.translate(this.posX, this.posY, this.posZ);
            this.transformMatrix.rotate(Math.toRadians(this.rotationYaw), 0.0F, 1.0F, 0.0F);
            this.transformMatrix.rotate(Math.toRadians(this.rotationPitch), 1.0F, 0.0F, 0.0F);
            this.transformMatrix.rotate(Math.toRadians(this.rotationRoll), 0.0F, 0.0F, 1.0F);

            this.untransformMatrix.setIdentity();
            this.untransformMatrix.multiply(this.transformMatrix);
            this.untransformMatrix.invert();
        }
    }

    public BlockPos getChunkPosition(BlockPos pos) {
        return new BlockPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    public BlockPos getPositionInChunk(BlockPos pos) {
        return new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }

    public Map<BlockPos, EntityChunk> getChunks() {
        return this.chunks;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        this.deserializing = true;
        this.rotationRoll = compound.getFloat("Roll");
        NBTTagList chunks = compound.getTagList("Chunks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < chunks.tagCount(); i++) {
            NBTTagCompound chunkData = chunks.getCompoundTagAt(i);
            BlockPos position = BlockPos.fromLong(chunkData.getLong("Position"));
            EntityChunk chunk = new EntityChunk(this, position);
            chunk.deserialize(chunkData);
            this.setChunk(position, chunk);
        }
        this.deserializing = false;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setFloat("Roll", this.rotationRoll);
        NBTTagList chunks = new NBTTagList();
        for (Map.Entry<BlockPos, EntityChunk> entry : this.chunks.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                NBTTagCompound chunkData = new NBTTagCompound();
                chunkData.setLong("Position", entry.getKey().toLong());
                entry.getValue().serialize(chunkData);
                chunks.appendTag(chunkData);
            }
        }
        compound.setTag("Chunks", chunks);
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        StructurePlayerHandler tracker = new StructurePlayerHandler(this, player);
        this.playerHandlers.put(player, tracker);
        for (Map.Entry<BlockPos, EntityChunk> chunk : this.chunks.entrySet()) {
            tracker.setDirty(chunk.getValue());
        }
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        this.playerHandlers.remove(player);
    }

    public void setChunk(BlockPos position, EntityChunk chunk) {
        EntityChunk previous = this.chunks.get(position);
        this.queuedChunks.add(new ChunkQueue(position, chunk, false));
        if (previous != null) {
            previous.unload();
            for (Map.Entry<EntityPlayer, StructurePlayerHandler> entry : this.playerHandlers.entrySet()) {
                entry.getValue().remove(previous);
            }
        }
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        EntityChunk chunk = this.chunks.get(this.getChunkPosition(pos));
        if (chunk != null) {
            return chunk.getTileEntity(this.getPositionInChunk(pos));
        }
        return null;
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        return 255;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        EntityChunk chunk = this.chunks.get(this.getChunkPosition(pos));
        if (chunk != null) {
            return chunk.getBlockState(this.getPositionInChunk(pos));
        }
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        IBlockState state = this.getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
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
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
        return this.getBlockState(pos).isSideSolid(this, pos, side);
    }

    public boolean setBlockState(BlockPos pos, IBlockState state) {
        BlockPos chunkPosition = this.getChunkPosition(pos);
        EntityChunk chunk = this.chunks.get(chunkPosition);
        if (chunk == null) {
            for (ChunkQueue queue : this.queuedChunks) {
                if (!queue.remove && queue.position.equals(chunkPosition)) {
                    chunk = queue.chunk;
                }
            }
        }
        if (chunk == null) {
            if (state.getBlock() == Blocks.AIR) {
                return false;
            }
            chunk = this.worldObj.isRemote ? new ClientEntityChunk(this, chunkPosition) : new EntityChunk(this, chunkPosition);
            this.setChunk(chunkPosition, chunk);
        }
        boolean success = chunk.setBlockState(this.getPositionInChunk(pos), state);
        if (chunk.isEmpty()) {
            chunk.unload();
            this.queuedChunks.add(new ChunkQueue(chunkPosition, chunk, true));
        }
        for (Map.Entry<EntityPlayer, StructurePlayerHandler> entry : this.playerHandlers.entrySet()) {
            entry.getValue().setDirty(chunk);
        }
        return success;
    }

    public Point3d getTransformedPosition(Point3d position) {
        position.sub(new Point3d(0.5, 0.0, 0.5));
        this.transformMatrix.transform(position);
        return position;
    }

    public Vec3d getTransformedPosition(Vec3d position) {
        Point3d point = new Point3d(position.xCoord - 0.5, position.yCoord, position.zCoord - 0.5);
        this.transformMatrix.transform(point);
        return new Vec3d(point.getX(), point.getY(), point.getZ());
    }

    public Point3d getUntransformedPosition(Point3d position) {
        this.untransformMatrix.transform(position);
        position.add(new Point3d(0.5, 0.0, 0.5));
        return position;
    }

    public Vec3d getUntransformedPosition(Vec3d position) {
        Point3d point = new Point3d(position.xCoord, position.yCoord, position.zCoord);
        this.untransformMatrix.transform(point);
        return new Vec3d(point.getX() + 0.5, point.getY(), point.getZ() + 0.5);
    }

    public Vec3d getTransformedVector(Vec3d vec) {
        Vector3d vector = new Vector3d(vec.xCoord, vec.yCoord, vec.zCoord);
        this.transformMatrix.transform(vector);
        return new Vec3d(vector.getX(), vector.getY(), vector.getZ());
    }

    public EntityChunk getChunkForBlock(BlockPos pos) {
        return this.chunks.get(this.getChunkPosition(pos));
    }

    public EntityChunk getChunk(BlockPos pos) {
        return this.chunks.get(pos);
    }

    private void rebuild() {
        for (Map.Entry<BlockPos, EntityChunk> entry : this.chunks.entrySet()) {
            if (entry.getValue() instanceof ClientEntityChunk) {
                ((ClientEntityChunk) entry.getValue()).rebuild();
            }
        }
    }

    public StructurePlayerHandler getPlayerHandler(EntityPlayer player) {
        return this.playerHandlers.get(player);
    }

    @Override
    public void setDead() {
        super.setDead();
        ClientEventHandler.handlers.remove(this);
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
