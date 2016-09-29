package net.starborne.server.message;

import io.netty.buffer.ByteBuf;
import net.ilexiconn.llibrary.server.network.AbstractMessage;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.starborne.server.entity.structure.StructureEntity;

public class InteractBlockEntityMessage extends AbstractMessage<InteractBlockEntityMessage> {
    private int entity;
    private BlockPos position;
    private float hitX;
    private float hitY;
    private float hitZ;
    private EnumHand hand;
    private EnumFacing side;

    public InteractBlockEntityMessage() {
    }

    public InteractBlockEntityMessage(int entity, BlockPos position, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        this.entity = entity;
        this.position = position;
        this.hand = hand;
        this.side = side;
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
    }

    @Override
    public void onClientReceived(Minecraft client, InteractBlockEntityMessage message, EntityPlayer player, MessageContext context) {
    }

    @Override
    public void onServerReceived(MinecraftServer server, InteractBlockEntityMessage message, EntityPlayer player, MessageContext context) {
        Entity entity = player.worldObj.getEntityByID(message.entity);
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            IBlockState state = structureEntity.getBlockState(message.position);
            ItemStack heldItem = player.getHeldItem(message.hand);
            if (!state.getBlock().onBlockActivated(structureEntity.structureWorld, message.position, state, player, message.hand, heldItem, message.side, message.hitX, message.hitY, message.hitZ)) {
                if (heldItem != null) {
                    int size = heldItem.stackSize;
                    heldItem.onItemUse(player, structureEntity.structureWorld, message.position, message.hand, message.side, message.hitX, message.hitY, message.hitZ);
                    if (player.capabilities.isCreativeMode && heldItem.stackSize < size) {
                        heldItem.stackSize = size;
                    }
                }
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entity = buf.readInt();
        this.position = BlockPos.fromLong(buf.readLong());
        this.hand = EnumHand.values()[buf.readByte()];
        this.side = EnumFacing.values()[buf.readByte()];
        this.hitX = buf.readFloat();
        this.hitY = buf.readFloat();
        this.hitZ = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeLong(this.position.toLong());
        buf.writeByte(this.hand.ordinal());
        buf.writeByte(this.side.ordinal());
        buf.writeFloat(this.hitX);
        buf.writeFloat(this.hitY);
        buf.writeFloat(this.hitZ);
    }

    @Override
    public boolean registerOnSide(Side side) {
        return side.isServer();
    }
}
