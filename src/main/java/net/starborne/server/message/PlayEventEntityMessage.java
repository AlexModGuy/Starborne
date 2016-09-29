package net.starborne.server.message;

import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.starborne.server.entity.structure.StructureEntity;

public class PlayEventEntityMessage extends AbstractMessage<PlayEventEntityMessage> {
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
    public void onClientReceived(Minecraft client, PlayEventEntityMessage message, EntityPlayer player, MessageContext context) {
        Entity entity = player.worldObj.getEntityByID(message.entity);
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            if (message.broadcast) {
                structureEntity.structureWorld.playBroadcastSound(message.type, message.position, message.data);
            } else {
                structureEntity.structureWorld.playEventServer(null, message.type, message.position, message.data);
            }
        }
    }

    @Override
    public void onServerReceived(MinecraftServer server, PlayEventEntityMessage message, EntityPlayer player, MessageContext context) {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entity = buf.readInt();
        this.position = BlockPos.fromLong(buf.readLong());
        this.type = buf.readInt();
        this.data = buf.readInt();
        this.broadcast = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeLong(this.position.toLong());
        buf.writeInt(this.type);
        buf.writeInt(this.data);
        buf.writeBoolean(this.broadcast);
    }

    @Override
    public boolean registerOnSide(Side side) {
        return side.isClient();
    }
}
