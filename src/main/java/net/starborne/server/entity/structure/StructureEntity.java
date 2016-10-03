package net.starborne.server.entity.structure;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.starborne.Starborne;
import net.starborne.server.entity.structure.world.StructureWorld;
import net.starborne.server.util.Matrix;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructureEntity extends Entity {
    public StructureWorld structureWorld;
    public float rotationRoll;
    public float prevRotationRoll;

    private Map<EntityPlayer, StructurePlayerHandler> playerHandlers;

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
        if (this.playerHandlers == null) {
            this.playerHandlers = new HashMap<>();
        }
        if (!this.worldObj.isRemote) {
            this.structureWorld.setBlockState(new BlockPos(0, 0, 0), Blocks.STONE.getDefaultState());
            this.structureWorld.updateChunkQueue();
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        this.prevRotationRoll = this.rotationRoll;

        this.structureWorld.tick();

        //TODO Improve trackers further
        List<EntityPlayer> remove = new ArrayList<>();

        for (Map.Entry<EntityPlayer, StructurePlayerHandler> entry : this.playerHandlers.entrySet()) {
            entry.getValue().update();
            if (entry.getKey().isDead) {
                remove.add(entry.getKey());
            }
        }

        for (EntityPlayer player : remove) {
            this.playerHandlers.remove(player);
        }

        this.rotationPitch = 25.0F;
//        this.rotationYaw += 0.1F;

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
            this.structureWorld.setChunk(position, chunk);
        }
        this.deserializing = false;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setFloat("Roll", this.rotationRoll);
        NBTTagList chunks = new NBTTagList();
        for (Map.Entry<BlockPos, EntityChunk> entry : this.structureWorld.getChunks().entrySet()) {
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
        for (Map.Entry<BlockPos, EntityChunk> chunk : this.structureWorld.getChunks().entrySet()) {
            tracker.setDirty(chunk.getValue());
        }
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        this.playerHandlers.remove(player);
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

    private void rebuild() {
        for (Map.Entry<BlockPos, EntityChunk> entry : this.structureWorld.getChunks().entrySet()) {
            if (entry.getValue() instanceof ClientEntityChunk) {
                ((ClientEntityChunk) entry.getValue()).rebuild();
            }
        }
    }

    @Override
    public void setDead() {
        super.setDead();
        Starborne.PROXY.getStructureHandler(this.worldObj).removeEntity(this);
    }

    public void addPlayerHandler(EntityPlayer player) {
        this.playerHandlers.put(player, new StructurePlayerHandler(this, player));
    }

    public StructurePlayerHandler getPlayerHandler(EntityPlayer player) {
        return this.playerHandlers.get(player);
    }

    public Map<EntityPlayer, StructurePlayerHandler> getPlayerHandlers() {
        return this.playerHandlers;
    }
}
