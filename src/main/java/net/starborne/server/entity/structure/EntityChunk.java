package net.starborne.server.entity.structure;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class EntityChunk {
    private static final Block EMPTY = Blocks.AIR;

    private final World world;
    private final BlockPos position;
    private final BlockStateContainer stateData;
    private final Map<BlockPos, TileEntity> tileEntities = new HashMap<>();

    private int blockCount;
    private int tickedBlockCount;

    public EntityChunk(World world, BlockPos position) {
        this.world = world;
        this.position = position;
        this.stateData = new BlockStateContainer();
    }

    public IBlockState getBlockState(BlockPos position) {
        return this.getBlockState(position.getX(), position.getY(), position.getZ());
    }

    public IBlockState getBlockState(int x, int y, int z) {
        return this.stateData.get(x, y, z);
    }

    public void setBlockState(BlockPos position, IBlockState state) {
        this.setBlockState(position.getX(), position.getY(), position.getZ(), state);
    }

    public void setBlockState(int x, int y, int z, IBlockState state) {
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
            this.tileEntities.put(position, block.createTileEntity(this.world, state));
        } else if (previousBlock.hasTileEntity(state)) {
            this.tileEntities.remove(position);
        }
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

    public void serialize(NBTTagCompound compound) {
        //TODO OPTIMIZE
        NBTTagList blocksList = new NBTTagList();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    blocksList.appendTag(new NBTTagInt(Block.getStateId(this.getBlockState(x, y, z))));
                }
            }
        }
        compound.setTag("Blocks", blocksList);
    }

    public void deserialize(NBTTagCompound compound) {
        NBTTagList blocks = compound.getTagList("Blocks", Constants.NBT.TAG_INT);
        int blockX = 0;
        int blockY = 0;
        int blockZ = 0;
        for (int i = 0; i < blocks.tagCount(); i++) {
            this.setBlockState(blockX, blockY, blockZ, Block.getStateById(blocks.getIntAt(i)));
            blockX++;
            if (blockX >= 16) {
                blockX = 0;
                blockY++;
                if (blockY >= 16) {
                    blockY = 0;
                    blockZ++;
                    if (blockZ >= 16) {
                        return;
                    }
                }
            }
        }
    }

    public TileEntity getTileEntity(BlockPos position) {
        return this.tileEntities.get(position);
    }
}
