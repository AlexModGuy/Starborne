package net.starborne.server.message;

import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.starborne.server.entity.structure.ClientEntityChunk;
import net.starborne.server.entity.structure.EntityChunk;
import net.starborne.server.entity.structure.StructureEntity;

public class EntityChunkMessage extends AbstractMessage<EntityChunkMessage> {
    private int entity;
    private BlockPos position;
    private NBTTagCompound data;

    public EntityChunkMessage() {
    }

    public EntityChunkMessage(int entity, EntityChunk chunk) {
        this.entity = entity;
        this.position = chunk.getPosition();
        this.data = new NBTTagCompound();
        chunk.serialize(this.data);
    }

    @Override
    public void onClientReceived(Minecraft client, EntityChunkMessage message, EntityPlayer player, MessageContext context) {
        Entity entity = client.theWorld.getEntityByID(message.entity);
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            EntityChunk chunk = new ClientEntityChunk(structureEntity, message.position);
            chunk.deserialize(message.data);
            structureEntity.structureWorld.setChunk(message.position, chunk);
        }
    }

    @Override
    public void onServerReceived(MinecraftServer server, EntityChunkMessage message, EntityPlayer player, MessageContext context) {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entity = buf.readInt();
        this.position = BlockPos.fromLong(buf.readLong());
        this.data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeLong(this.position.toLong());
        ByteBufUtils.writeTag(buf, this.data);
    }

    @Override
    public boolean registerOnSide(Side side) {
        return side.isClient();
    }
}
