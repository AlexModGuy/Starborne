package net.starborne.server.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.starborne.Starborne;

public abstract class BaseMessage<T extends BaseMessage<T>> implements IMessage, IMessageHandler<T, IMessage> {
    @Override
    public void fromBytes(ByteBuf buf) {
        this.deserialize(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        this.serialize(buf);
    }

    public abstract void serialize(ByteBuf buf);
    public abstract void deserialize(ByteBuf buf);

    public abstract void onReceiveClient(Minecraft client, WorldClient world, EntityPlayerSP player, MessageContext context);

    public abstract void onReceiveServer(MinecraftServer server, WorldServer world, EntityPlayerMP player, MessageContext context);

    @Override
    public IMessage onMessage(T message, MessageContext context) {
        Starborne.PROXY.handleMessage(message, context);
        return null;
    }
}
