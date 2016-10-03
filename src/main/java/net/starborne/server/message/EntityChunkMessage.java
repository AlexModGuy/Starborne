package net.starborne.server.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.starborne.server.entity.structure.ClientEntityChunk;
import net.starborne.server.entity.structure.EntityChunk;
import net.starborne.server.entity.structure.StructureEntity;

public class EntityChunkMessage extends BaseMessage {
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
    public void serialize(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeLong(this.position.toLong());
        ByteBufUtils.writeTag(buf, this.data);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        this.entity = buf.readInt();
        this.position = BlockPos.fromLong(buf.readLong());
        this.data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void onReceiveClient(Minecraft client, WorldClient world, EntityPlayerSP player, MessageContext context) {
        Entity entity = client.theWorld.getEntityByID(this.entity);
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            EntityChunk chunk = new ClientEntityChunk(structureEntity, this.position);
            chunk.deserialize(this.data);
            structureEntity.structureWorld.setChunk(this.position, chunk);
        }
    }

    @Override
    public void onReceiveServer(MinecraftServer server, WorldServer world, EntityPlayerMP player, MessageContext context) {

    }
}
