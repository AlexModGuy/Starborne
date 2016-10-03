package net.starborne.server.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.starborne.server.entity.structure.StructureEntity;

public class InteractBlockEntityMessage extends BaseMessage {
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
    public void serialize(ByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeLong(this.position.toLong());
        buf.writeByte(this.hand.ordinal());
        buf.writeByte(this.side.ordinal());
        buf.writeFloat(this.hitX);
        buf.writeFloat(this.hitY);
        buf.writeFloat(this.hitZ);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        this.entity = buf.readInt();
        this.position = BlockPos.fromLong(buf.readLong());
        this.hand = EnumHand.values()[buf.readByte()];
        this.side = EnumFacing.values()[buf.readByte()];
        this.hitX = buf.readFloat();
        this.hitY = buf.readFloat();
        this.hitZ = buf.readFloat();
    }

    @Override
    public void onReceiveClient(Minecraft client, WorldClient world, EntityPlayerSP player, MessageContext context) {

    }

    @Override
    public void onReceiveServer(MinecraftServer server, WorldServer world, EntityPlayerMP player, MessageContext context) {
        Entity entity = player.worldObj.getEntityByID(this.entity);
        if (entity instanceof StructureEntity) {
            StructureEntity structureEntity = (StructureEntity) entity;
            IBlockState state = structureEntity.structureWorld.getBlockState(this.position);
            ItemStack heldItem = player.getHeldItem(this.hand);
            if (!state.getBlock().onBlockActivated(structureEntity.structureWorld, this.position, state, player, this.hand, heldItem, this.side, this.hitX, this.hitY, this.hitZ)) {
                if (heldItem != null) {
                    int size = heldItem.stackSize;
                    heldItem.onItemUse(player, structureEntity.structureWorld, this.position, this.hand, this.side, this.hitX, this.hitY, this.hitZ);
                    if (player.capabilities.isCreativeMode && heldItem.stackSize < size) {
                        heldItem.stackSize = size;
                    }
                    if (heldItem.stackSize <= 0) {
                        player.setHeldItem(this.hand, null);
                        ForgeEventFactory.onPlayerDestroyItem(player, heldItem, this.hand);
                    }
                }
            }
        }
    }
}
