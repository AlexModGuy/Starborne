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
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.StructurePlayerHandler;

public class BreakBlockEntityMessage extends AbstractMessage<BreakBlockEntityMessage> {
    private int entity;
    private BlockPos position;
    private BreakState breakState;

    public BreakBlockEntityMessage() {
    }

    public BreakBlockEntityMessage(int entity, BlockPos position, BreakState breakState) {
        this.entity = entity;
        this.position = position;
        this.breakState = breakState;
    }

    @Override
    public void onClientReceived(Minecraft client, BreakBlockEntityMessage message, EntityPlayer player, MessageContext context) {
    }

    @Override
    public void onServerReceived(MinecraftServer server, BreakBlockEntityMessage message, EntityPlayer player, MessageContext context) {
        Entity entity = player.worldObj.getEntityByID(message.entity);
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            IBlockState state = structureEntity.structureWorld.getBlockState(message.position);
            StructurePlayerHandler playerHandler = structureEntity.getPlayerHandler(player);
            if (message.breakState == BreakState.BREAK) {
                if (player.capabilities.isCreativeMode || (playerHandler.getBreaking() != null && playerHandler.getBreakProgress() >= 1.0F)) {
                    structureEntity.structureWorld.setBlockState(message.position, Blocks.AIR.getDefaultState());
                }
            } else if (message.breakState == BreakState.START) {
                playerHandler.startBreaking(message.position);
            } else {
                playerHandler.startBreaking(null);
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entity = buf.readInt();
        this.position = BlockPos.fromLong(buf.readLong());
        this.breakState = BreakState.values()[buf.readByte()];
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeLong(this.position.toLong());
        buf.writeByte(this.breakState.ordinal());
    }

    public enum BreakState {
        START,
        STOP,
        BREAK
    }
}
