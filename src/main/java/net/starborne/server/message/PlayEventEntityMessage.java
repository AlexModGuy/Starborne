package net.starborne.server.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.starborne.server.entity.structure.StructureEntity;

public class PlayEventEntityMessage extends BaseMessage {
    private int entity;
    private BlockPos position;
    private int type;
    private int data;
    private boolean broadcast;

    public PlayEventEntityMessage() {
    }

    public PlayEventEntityMessage(int entity, BlockPos position, int type, int data, boolean broadcast) {
        this.entity = entity;
        this.position = position;
        this.type = type;
        this.data = data;
        this.broadcast = broadcast;
    }

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeLong(this.position.toLong());
        buf.writeInt(this.type);
        buf.writeInt(this.data);
        buf.writeBoolean(this.broadcast);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        this.entity = buf.readInt();
        this.position = BlockPos.fromLong(buf.readLong());
        this.type = buf.readInt();
        this.data = buf.readInt();
        this.broadcast = buf.readBoolean();
    }

    @Override
    public void onReceiveClient(Minecraft client, WorldClient world, EntityPlayerSP player, MessageContext context) {
        Entity entity = player.worldObj.getEntityByID(this.entity);
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            if (this.broadcast) {
                structureEntity.structureWorld.playBroadcastSound(this.type, this.position, this.data);
            } else {
                structureEntity.structureWorld.playEventServer(null, this.type, this.position, this.data);
            }
        }
    }

    @Override
    public void onReceiveServer(MinecraftServer server, WorldServer world, EntityPlayerMP player, MessageContext context) {
    }
}
