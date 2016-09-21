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
import net.starborne.Starborne;
import net.starborne.server.biome.BiomeHandler;
import net.starborne.server.message.EntityChunkMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructureEntity extends Entity implements IBlockAccess {
    public float rotationRoll;

    private Map<BlockPos, EntityChunk> chunks;
    private List<EntityPlayerMP> tracking = new ArrayList<>();
    private boolean dirty;

    public StructureEntity(World world) {
        super(world);
    }

    @Override
    protected void entityInit() {
        this.chunks = new HashMap<>();
        this.setBlockState(new BlockPos(0, 0, 0), Blocks.STONE.getDefaultState());
        this.setBlockState(new BlockPos(0, 1, 0), Blocks.DIAMOND_BLOCK.getDefaultState());
        this.setBlockState(new BlockPos(0, 2, 0), Blocks.GRASS.getDefaultState());
        this.setBlockState(new BlockPos(0, 3, 0), Blocks.TORCH.getDefaultState());
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.dirty) {
            if (this.tracking.size() > 0) {
                for (EntityPlayerMP player : this.tracking) {
                    this.syncChunks(player);
                }
                this.dirty = false;
            }
        }
        this.rotationYaw += 1.0F;
    }

    private void syncChunks(EntityPlayerMP player) {
        //TODO more efficient way of sending chunks - single at a time and only when in range
        for (Map.Entry<BlockPos, EntityChunk> entry : this.chunks.entrySet()) {
            Starborne.networkWrapper.sendTo(new EntityChunkMessage(this.getEntityId(), entry.getValue()), player);
        }
    }

    public BlockPos getChunkPosition(BlockPos pos) {
        return new BlockPos(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    public BlockPos getPositionInChunk(BlockPos pos) {
        return new BlockPos(pos.getX() & 16, pos.getY() & 16, pos.getZ() & 16);
    }

    public Map<BlockPos, EntityChunk> getChunks() {
        return this.chunks;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        this.rotationRoll = compound.getFloat("Roll");
        NBTTagList chunks = compound.getTagList("Chunks", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < chunks.tagCount(); i++) {
            NBTTagCompound chunkData = chunks.getCompoundTagAt(i);
            BlockPos position = BlockPos.fromLong(chunkData.getLong("Position"));
            EntityChunk chunk = new EntityChunk(this.worldObj, position);
            chunk.deserialize(chunkData);
            this.chunks.put(position, chunk);
        }
        this.dirty = true;
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
        this.tracking.add(player);
        if (!this.dirty) {
            this.syncChunks(player);
        }
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        this.tracking.remove(player);
    }

    public void setChunk(BlockPos position, EntityChunk chunk) {
        this.chunks.put(position, chunk);
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

    public void setBlockState(BlockPos pos, IBlockState state) {
        BlockPos chunkPosition = this.getChunkPosition(pos);
        EntityChunk chunk = this.chunks.get(chunkPosition);
        if (chunk == null) {
            if (state.getBlock() == Blocks.AIR) {
                return;
            }
            chunk = new EntityChunk(this.worldObj, chunkPosition);
            this.chunks.put(chunkPosition, chunk);
        }
        chunk.setBlockState(pos, state);
        if (chunk.isEmpty()) {
            this.chunks.remove(chunkPosition);
        }
        this.dirty = true;
    }
}
