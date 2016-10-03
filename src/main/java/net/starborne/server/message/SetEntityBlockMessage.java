package net.starborne.server.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
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

public class SetEntityBlockMessage extends BaseMessage {
    private int entity;
    private BlockPos position;
    private IBlockState state;

    public SetEntityBlockMessage() {
    }

    public SetEntityBlockMessage(int entity, BlockPos position, IBlockState state) {
        this.entity = entity;
        this.position = position;
        this.state = state;
    }

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeLong(this.position.toLong());
        buf.writeInt(Block.getStateId(this.state));
    }

    @Override
    public void deserialize(ByteBuf buf) {
        this.entity = buf.readInt();
        this.position = BlockPos.fromLong(buf.readLong());
        this.state = Block.getStateById(buf.readInt());
    }

    @Override
    public void onReceiveClient(Minecraft client, WorldClient world, EntityPlayerSP player, MessageContext context) {
        Entity entity = client.theWorld.getEntityByID(this.entity);
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            structureEntity.structureWorld.setBlockState(this.position, this.state);
        }
    }

    @Override
    public void onReceiveServer(MinecraftServer server, WorldServer world, EntityPlayerMP player, MessageContext context) {
    }
}
