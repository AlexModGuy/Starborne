package net.starborne.server.entity.structure;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EntityChunk {
    protected static final Block EMPTY = Blocks.AIR;

    protected final StructureEntity entity;
    protected final World mainWorld;
    protected final World structureWorld;
    protected final BlockPos position;
    protected final BlockStateContainer stateData;
    protected final Map<BlockPos, TileEntity> tileEntities = new HashMap<>();
    protected final List<ITickable> tickables = new ArrayList<>();

    protected int blockCount;
    protected int tickedBlockCount;

    protected int updatePosition = new Random().nextInt();

    protected boolean loading;

    public EntityChunk(StructureEntity entity, BlockPos position) {
        this.entity = entity;
        this.mainWorld = entity.worldObj;
        this.structureWorld = entity.structureWorld;
        this.position = position;
        this.stateData = new BlockStateContainer();
    }

    public IBlockState getBlockState(BlockPos position) {
        return this.getBlockState(position.getX(), position.getY(), position.getZ());
    }

    public IBlockState getBlockState(int x, int y, int z) {
        return this.stateData.get(x, y, z);
    }

    public boolean setBlockState(BlockPos position, IBlockState state) {
        return this.setBlockState(position.getX(), position.getY(), position.getZ(), state);
    }

    public boolean setBlockState(int x, int y, int z, IBlockState state) {
        if (state instanceof IExtendedBlockState) {
            state = ((IExtendedBlockState) state).getClean();
        }
        IBlockState previousState = this.getBlockState(x, y, z);
        Block previousBlock = previousState.getBlock();
        Block block = state.getBlock();
        if (previousBlock != EMPTY) {
            this.blockCount--;
            if (previousBlock.getTickRandomly()) {
                this.tickedBlockCount--;
            }
        }
        if (block != EMPTY) {
            this.blockCount++;
            if (block.getTickRandomly()) {
                this.tickedBlockCount++;
            }
        }
        this.stateData.set(x, y, z, state);
        BlockPos position = new BlockPos(x, y, z);
        if (block.hasTileEntity(state)) {
            TileEntity tile = block.createTileEntity(this.structureWorld, state);
            this.tileEntities.put(position, tile);
            tile.setWorldObj(this.structureWorld);
            tile.setPos(new BlockPos((this.position.getX() << 4) + x, (this.position.getY() << 4) + y, (this.position.getZ() << 4) + z));
            if (tile instanceof ITickable) {
                this.tickables.add((ITickable) tile);
            }
        } else if (previousBlock.hasTileEntity(state)) {
            TileEntity tile = this.tileEntities.remove(position);
            if (tile instanceof ITickable) {
                this.tickables.remove(tile);
            }
        }
        return previousState != state;
    }

    public boolean isEmpty() {
        return this.blockCount == 0;
    }

    public boolean shouldTick() {
        return this.tickedBlockCount > 0;
    }

    public void recalculateBlocks() {
        this.blockCount = 0;
        this.tickedBlockCount = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    Block block = this.getBlockState(x, y, z).getBlock();
                    if (block != EMPTY) {
                        this.blockCount++;
                        if (block.getTickRandomly()) {
                            this.tickedBlockCount++;
                        }
                    }
                }
            }
        }
    }

    public BlockPos getPosition() {
        return this.position;
    }

    public TileEntity getTileEntity(BlockPos position) {
        return this.tileEntities.get(position);
    }

    public void serialize(NBTTagCompound compound) {
        List<SaveEntry> entries = new ArrayList<>();
        for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    SaveEntry previousEntry = entries.size() > 0 ? entries.get(entries.size() - 1) : null;
                    IBlockState state = this.getBlockState(x, y, z);
                    if (previousEntry == null || previousEntry.state != state) {
                        entries.add(new SaveEntry(state));
                    } else {
                        previousEntry.repeats++;
                    }
                }
            }
        }
        NBTTagList blocksList = new NBTTagList();
        for (SaveEntry entry : entries) {
            NBTTagCompound entryTag = new NBTTagCompound();
            if (entry.repeats > 1) {
                entryTag.setShort("R", (short) entry.repeats);
            }
            entryTag.setInteger("I", Block.getStateId(entry.state));
            blocksList.appendTag(entryTag);
        }
        compound.setTag("Blocks", blocksList);
    }

    public void deserialize(NBTTagCompound compound) {
        this.loading = true;
        this.blockCount = 0;
        this.tickedBlockCount = 0;
        NBTTagList blocks = compound.getTagList("Blocks", Constants.NBT.TAG_COMPOUND);
        int blockX = 0;
        int blockY = 0;
        int blockZ = 0;
        for (int i = 0; i < blocks.tagCount(); i++) {
            NBTTagCompound entryTag = blocks.getCompoundTagAt(i);
            int repeats = entryTag.hasKey("R") ? entryTag.getShort("R") : 1;
            int id = entryTag.getInteger("I");
            IBlockState state = Block.getStateById(id);
            for (int repeat = 0; repeat < repeats; repeat++) {
                this.setBlockState(blockX, blockY, blockZ, state);
                blockX++;
                if (blockX >= 16) {
                    blockX = 0;
                    blockY++;
                    if (blockY >= 16) {
                        blockY = 0;
                        blockZ++;
                    }
                }
            }
        }
        this.loading = false;
    }

    public void unload() {
    }

    public void update() {
        if (this.tickedBlockCount > 0) {
            int tickSpeed = this.mainWorld.getGameRules().getInt("randomTickSpeed");
            for (int i = 0; i < tickSpeed; i++) {
                this.updatePosition = this.updatePosition * 3 + 0x3C6EF35F;
                int position = this.updatePosition >> 2;
                int x = position & 15;
                int y = position >> 8 & 15;
                int z = position >> 16 & 15;
                IBlockState state = this.stateData.get(x, y, z);
                Block block = state.getBlock();
                if (block.getTickRandomly()) {
                    BlockPos pos = new BlockPos((this.position.getX() << 4) + x, (this.position.getY() << 4) + y, (this.position.getZ() << 4) + z);
                    block.randomTick(this.structureWorld, pos, state, this.structureWorld.rand);
                }
            }
        }
        for (ITickable tickable : this.tickables) {
            tickable.update();
        }
    }

    protected class SaveEntry {
        protected int repeats;
        protected IBlockState state;

        protected SaveEntry(IBlockState state) {
            this.state = state;
            this.repeats = 1;
        }
    }
}
