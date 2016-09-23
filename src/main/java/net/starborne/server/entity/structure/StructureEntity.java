package net.starborne.server.entity.structure;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.util.Constants;
import net.starborne.server.biome.BiomeHandler;
import net.starborne.server.entity.structure.world.StructureWorld;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StructureEntity extends Entity implements IBlockAccess {
    public StructureWorld structureWorld;
    public float rotationRoll;

    private Map<BlockPos, EntityChunk> chunks;
    private Map<EntityPlayerMP, EntityChunkTracker> trackers;

    public StructureEntity(World world) {
        super(world);
    }

    @Override
    protected void entityInit() {
        if (this.structureWorld == null) {
            this.structureWorld = new StructureWorld(this);
        }

        if (this.chunks == null) {
            this.chunks = Collections.synchronizedMap(new HashMap<>());
        }

        if (this.trackers == null) {
            this.trackers = new HashMap<>();
        }

        if (!this.worldObj.isRemote) {
            this.setBlockState(new BlockPos(0, 0, 0), Blocks.STONE.getDefaultState());
            this.setBlockState(new BlockPos(0, 1, 0), Blocks.DIAMOND_BLOCK.getDefaultState());
            this.setBlockState(new BlockPos(0, 2, 0), Blocks.GRASS.getDefaultState());
            this.setBlockState(new BlockPos(0, 3, 0), Blocks.TORCH.getDefaultState());

            this.setBlockState(new BlockPos(1, 1, 0), Blocks.FLOWING_WATER.getDefaultState());
            this.setBlockState(new BlockPos(0, 1, 1), Blocks.FLOWING_LAVA.getDefaultState());

            this.setBlockState(new BlockPos(-1, 1, 0), Blocks.ICE.getDefaultState());
            this.setBlockState(new BlockPos(0, 1, -1), Blocks.CHEST.getDefaultState());
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.worldObj.isRemote) {
            //TODO Improve trackers further
            for (Map.Entry<EntityPlayerMP, EntityChunkTracker> entry : this.trackers.entrySet()) {
                entry.getValue().update();
            }
            //TODO only update tracked chunks
            synchronized (this.chunks) {
                for (Map.Entry<BlockPos, EntityChunk> entry : this.chunks.entrySet()) {
                    entry.getValue().update();
                }
            }
        }
        this.rotationPitch += 1.0F;
        this.rotationYaw += 1.0F;
    }

    public BlockPos getChunkPosition(BlockPos pos) {
        return new BlockPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    public BlockPos getPositionInChunk(BlockPos pos) {
        return new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }

    public Map<BlockPos, EntityChunk> getChunks() {
        synchronized (this.chunks) {
            return this.chunks;
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        this.rotationRoll = compound.getFloat("Roll");
        NBTTagList chunks = compound.getTagList("Chunks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < chunks.tagCount(); i++) {
            NBTTagCompound chunkData = chunks.getCompoundTagAt(i);
            BlockPos position = BlockPos.fromLong(chunkData.getLong("Position"));
            EntityChunk chunk = new EntityChunk(this, position);
            chunk.deserialize(chunkData);
            this.setChunk(position, chunk);
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setFloat("Roll", this.rotationRoll);
        NBTTagList chunks = new NBTTagList();
        synchronized (this.chunks) {
            for (Map.Entry<BlockPos, EntityChunk> entry : this.chunks.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    NBTTagCompound chunkData = new NBTTagCompound();
                    chunkData.setLong("Position", entry.getKey().toLong());
                    entry.getValue().serialize(chunkData);
                    chunks.appendTag(chunkData);
                }
            }
        }
        compound.setTag("Chunks", chunks);
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        EntityChunkTracker tracker = new EntityChunkTracker(this, player);
        this.trackers.put(player, tracker);
        synchronized (this.chunks) {
            for (Map.Entry<BlockPos, EntityChunk> chunk : this.chunks.entrySet()) {
                tracker.setDirty(chunk.getValue());
            }
        }
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        this.trackers.remove(player);
    }

    public void setChunk(BlockPos position, EntityChunk chunk) {
        synchronized (this.chunks) {
            EntityChunk previous = this.chunks.get(position);
            this.chunks.put(position, chunk);
            if (previous != null) {
                previous.unload();
            }
        }
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        synchronized (this.chunks) {
            EntityChunk chunk = this.chunks.get(this.getChunkPosition(pos));
            if (chunk != null) {
                return chunk.getTileEntity(this.getPositionInChunk(pos));
            }
        }
        return null;
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        return 255;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        synchronized (this.chunks) {
            EntityChunk chunk = this.chunks.get(this.getChunkPosition(pos));
            if (chunk != null) {
                return chunk.getBlockState(this.getPositionInChunk(pos));
            }
        }
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        IBlockState state = this.getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    public Biome getBiomeGenForCoords(BlockPos pos) {
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
        EntityChunk chunk;
        synchronized (this.chunks) {
            chunk = this.chunks.get(chunkPosition);
        }
        if (chunk == null) {
            if (state.getBlock() == Blocks.AIR) {
                return false;
            }
            chunk = new EntityChunk(this, chunkPosition);
            this.setChunk(chunkPosition, chunk);
        }
        boolean success = chunk.setBlockState(this.getPositionInChunk(pos), state);
        if (chunk.isEmpty()) {
            chunk.unload();
            synchronized (this.chunks) {
                this.chunks.remove(chunkPosition);
            }
        }
        for (Map.Entry<EntityPlayerMP, EntityChunkTracker> entry : this.trackers.entrySet()) {
            entry.getValue().setDirty(chunk);
        }
        return success;
    }
}
