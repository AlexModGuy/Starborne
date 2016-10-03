package net.starborne.server.world.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.util.Constants;

import java.util.HashSet;
import java.util.Set;

public class StarborneWorldSavedData extends WorldSavedData {
    public static final String KEY = "starborne";

    private Set<BlockPos> occupiedWorldChunks = new HashSet<>();

    public StarborneWorldSavedData() {
        this(KEY);
    }

    public StarborneWorldSavedData(String name) {
        super(name);
    }

    public static StarborneWorldSavedData get(World world) {
        MapStorage storage = world.getPerWorldStorage();
        StarborneWorldSavedData data = (StarborneWorldSavedData) storage.getOrLoadData(StarborneWorldSavedData.class, KEY);
        if (data == null) {
            data = new StarborneWorldSavedData();
            storage.setData(KEY, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        NBTTagList occupiedList = compound.getTagList("Occupied", Constants.NBT.TAG_LONG);
        for (int i = 0; i < occupiedList.tagCount(); i++) {
            this.occupiedWorldChunks.add(BlockPos.fromLong(((NBTTagLong) occupiedList.get(i)).getLong()));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList occupiedList = new NBTTagList();
        for (BlockPos pos : this.occupiedWorldChunks) {
            occupiedList.appendTag(new NBTTagLong(pos.toLong()));
        }
        compound.setTag("Occupied", occupiedList);
        return compound;
    }

    public void occupyChunk(BlockPos pos) {
        if (!this.occupiedWorldChunks.contains(pos)) {
            this.occupiedWorldChunks.add(pos);
            this.markDirty();
        }
    }

    public void unoccupyChunk(BlockPos pos) {
        if (this.occupiedWorldChunks.remove(pos)) {
            this.markDirty();
        }
    }

    public boolean isOccupied(BlockPos pos) {
        return this.occupiedWorldChunks.contains(pos);
    }
}
