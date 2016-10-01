package net.starborne.server.message;

import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.starborne.server.entity.structure.StructureEntity;

public class SetEntityBlockMessage extends AbstractMessage<SetEntityBlockMessage> {
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
    public void onClientReceived(Minecraft client, SetEntityBlockMessage message, EntityPlayer player, MessageContext context) {
        Entity entity = client.theWorld.getEntityByID(message.entity);
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            structureEntity.structureWorld.setBlockState(message.position, message.state);
        }
    }

    @Override
    public void onServerReceived(MinecraftServer server, SetEntityBlockMessage message, EntityPlayer player, MessageContext context) {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entity = buf.readInt();
        this.position = BlockPos.fromLong(buf.readLong());
        this.state = Block.getStateById(buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeLong(this.position.toLong());
        buf.writeInt(Block.getStateId(this.state));
    }

    @Override
    public boolean registerOnSide(Side side) {
        return side.isClient();
    }
}
