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
import net.starborne.server.entity.structure.StructurePlayerHandler;

public class BreakBlockEntityMessage extends BaseMessage {
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
    public void serialize(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeLong(this.position.toLong());
        buf.writeByte(this.breakState.ordinal());
    }

    @Override
    public void deserialize(ByteBuf buf) {
        this.entity = buf.readInt();
        this.position = BlockPos.fromLong(buf.readLong());
        this.breakState = BreakState.values()[buf.readByte()];
    }

    @Override
    public void onReceiveClient(Minecraft client, WorldClient world, EntityPlayerSP player, MessageContext context) {

    }

    @Override
    public void onReceiveServer(MinecraftServer server, WorldServer world, EntityPlayerMP player, MessageContext context) {
        Entity entity = player.worldObj.getEntityByID(this.entity);
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            StructurePlayerHandler playerHandler = structureEntity.getPlayerHandler(player);
            if (this.breakState == BreakState.BREAK) {
                if (player.capabilities.isCreativeMode || ((playerHandler.getBreaking() != null && playerHandler.getBreakProgress() >= 1.0F) || playerHandler.getLastBroken() != null)) {
                    playerHandler.breakBlock(this.position);
                    playerHandler.startBreaking(null);
                    playerHandler.clearLastBroken();
                }
            } else if (this.breakState == BreakState.START) {
                playerHandler.startBreaking(this.position);
                structureEntity.structureWorld.getBlockState(this.position).getBlock().onBlockClicked(structureEntity.structureWorld, this.position, player);
            } else {
                playerHandler.startBreaking(null);
            }
        }
    }

    public enum BreakState {
        START,
        STOP,
        BREAK
    }
}
