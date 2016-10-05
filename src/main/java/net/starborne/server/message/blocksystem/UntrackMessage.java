package net.starborne.server.message.blocksystem;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.starborne.Starborne;
import net.starborne.server.blocksystem.BlockSystem;
import net.starborne.server.message.BaseMessage;

public class UntrackMessage extends BaseMessage<UntrackMessage> {
    private int blockSystem;

    public UntrackMessage() {
    }

    public UntrackMessage(BlockSystem blockSystem) {
        this.blockSystem = blockSystem.getID();
    }

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeInt(this.blockSystem);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        this.blockSystem = buf.readInt();
    }

    @Override
    public void onReceiveClient(Minecraft client, WorldClient world, EntityPlayerSP player, MessageContext context) {
        Starborne.PROXY.getBlockSystemHandler(world).removeBlockSystem(this.blockSystem);
    }

    @Override
    public void onReceiveServer(MinecraftServer server, WorldServer world, EntityPlayerMP player, MessageContext context) {
    }
}
