package net.starborne;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.starborne.server.ServerProxy;
import net.starborne.server.core.StarbornePlugin;
import net.starborne.server.message.BaseMessage;
import net.starborne.server.message.BreakBlockEntityMessage;
import net.starborne.server.message.EntityChunkMessage;
import net.starborne.server.message.InteractBlockEntityMessage;
import net.starborne.server.message.PlayEventEntityMessage;
import net.starborne.server.message.SetEntityBlockMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Starborne.MODID, name = "Starborne", version = Starborne.VERSION)
public class Starborne {
    public static final String MODID = "starborne";
    public static final String VERSION = "0.1.0";

    @Mod.Instance(Starborne.MODID)
    public static Starborne INSTANCE;

    @SidedProxy(clientSide = "net.starborne.client.ClientProxy", serverSide = "net.starborne.server.ServerProxy")
    public static ServerProxy PROXY;

    public static final Logger LOGGER = LogManager.getLogger("Starborne");

    public static final SimpleNetworkWrapper NETWORK_WRAPPER = new SimpleNetworkWrapper(Starborne.MODID);

    private static int messageID;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        if (!StarbornePlugin.loaded) {
            System.err.println("Failed to load Starborne! Missing coremod parameters! (-Dfml.coreMods.load=net.starborne.server.core.StarbornePlugin)");
            FMLCommonHandler.instance().exitJava(1, false);
        }

        Starborne.registerMessage(BreakBlockEntityMessage.class, Side.SERVER);
        Starborne.registerMessage(EntityChunkMessage.class, Side.CLIENT);
        Starborne.registerMessage(InteractBlockEntityMessage.class, Side.SERVER);
        Starborne.registerMessage(PlayEventEntityMessage.class, Side.CLIENT);
        Starborne.registerMessage(SetEntityBlockMessage.class, Side.CLIENT);

        Starborne.PROXY.onPreInit();
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        Starborne.PROXY.onInit();
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {
        Starborne.PROXY.onPostInit();
    }

    private static <T extends BaseMessage<T> & IMessageHandler<T, IMessage>> void registerMessage(Class<T> message, Side... sides) {
        for (Side side : sides) {
            NETWORK_WRAPPER.registerMessage(message, message, messageID++, side);
        }
    }
}
