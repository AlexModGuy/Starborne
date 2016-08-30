package net.starborne;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Starborne.MODID, name = "Starborne", version = Starborne.VERSION, dependencies = "required-after:llibrary@[" + Starborne.LLIBRARY_VERSION + ",)")
public class Starborne {
    public static final String MODID = "starborne";
    public static final String VERSION = "0.1.0";
    public static final String LLIBRARY_VERSION = "1.5.1";

    @Mod.Instance(Starborne.MODID)
    public static Starborne INSTANCE;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {

    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {

    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {

    }
}
