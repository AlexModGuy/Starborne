package net.starborne.server.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.starborne.Starborne;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.StructurePlayerHandler;

public class BreakBlockEntityMessage extends BaseMessage {
    private int entity;
    private BlockPos position;
    private BreakState breakState;
    private int player;

    public BreakBlockEntityMessage() {
    }

    public BreakBlockEntityMessage(EntityPlayer player, int entity, BlockPos position, BreakState breakState) {
        this.entity = entity;
        this.position = position;
        this.breakState = breakState;
        this.player = player.getEntityId();
    }

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeInt(this.player);
        buf.writeByte(this.breakState.ordinal());
        if (this.breakState != BreakState.STOP) {
            buf.writeLong(this.position.toLong());
        }
    }

    @Override
    public void deserialize(ByteBuf buf) {
        this.entity = buf.readInt();
        this.player = buf.readInt();
        this.breakState = BreakState.values()[buf.readByte()];
        if (this.breakState != BreakState.STOP) {
            this.position = BlockPos.fromLong(buf.readLong());
        }
    }

    @Override
    public void onReceiveClient(Minecraft client, WorldClient world, EntityPlayerSP clientPlayer, MessageContext context) {
        Entity entity = world.getEntityByID(this.entity);
        Entity playerEntity = world.getEntityByID(this.player);
        if (entity instanceof StructureEntity && playerEntity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) playerEntity;
            StructureEntity structureEntity = (StructureEntity) entity;
            StructurePlayerHandler playerHandler = structureEntity.getPlayerHandler(player);
            if (this.breakState == BreakState.START) {
                playerHandler.startBreaking(this.position);
                structureEntity.structureWorld.getBlockState(this.position).getBlock().onBlockClicked(structureEntity.structureWorld, this.position, player);
            } else if (this.breakState == BreakState.STOP) {
                playerHandler.startBreaking(null);
            } else if (this.breakState == BreakState.BREAK) {
                playerHandler.breakBlock(this.position);
                playerHandler.startBreaking(null);
            }
        }
    }

    @Override
    public void onReceiveServer(MinecraftServer server, WorldServer world, EntityPlayerMP player, MessageContext context) {
        Entity entity = player.worldObj.getEntityByID(this.entity);
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            StructurePlayerHandler playerHandler = structureEntity.getPlayerHandler(player);
            if (this.breakState == BreakState.BREAK) {
                if (player.capabilities.isCreativeMode || ((playerHandler.getBreaking() != null && playerHandler.getBreakProgress() >= 0.9F) || playerHandler.getLastBroken() != null)) {
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
            for (EntityPlayer tracking : world.getEntityTracker().getTrackingPlayers(player)) {
                if (tracking instanceof EntityPlayerMP && tracking != player) {
                    Starborne.NETWORK_WRAPPER.sendTo(new BreakBlockEntityMessage(player, structureEntity.getEntityId(), this.position, this.breakState), (EntityPlayerMP) tracking);
                }
            }
        }
    }

    public enum BreakState {
        START,
        STOP,
        BREAK
    }
}
