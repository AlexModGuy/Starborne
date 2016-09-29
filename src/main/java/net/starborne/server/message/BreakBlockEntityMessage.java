package net.starborne.server.message;

import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.starborne.server.entity.structure.StructureEntity;

public class BreakBlockEntityMessage extends AbstractMessage<BreakBlockEntityMessage> {
    private int entity;
    private BlockPos position;

    public BreakBlockEntityMessage() {
    }

    public BreakBlockEntityMessage(int entity, BlockPos position) {
        this.entity = entity;
        this.position = position;
    }

    @Override
    public void onClientReceived(Minecraft client, BreakBlockEntityMessage message, EntityPlayer player, MessageContext context) {
    }

    @Override
    public void onServerReceived(MinecraftServer server, BreakBlockEntityMessage message, EntityPlayer player, MessageContext context) {
        Entity entity = player.worldObj.getEntityByID(message.entity);
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            IBlockState state = structureEntity.getBlockState(message.position);
            if (player.capabilities.isCreativeMode) {
                structureEntity.setBlockState(message.position, Blocks.AIR.getDefaultState());
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entity = buf.readInt();
        this.position = BlockPos.fromLong(buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeLong(this.position.toLong());
    }

    @Override
    public boolean registerOnSide(Side side) {
        return side.isServer();
    }
}
