package net.starborne.server.biome;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.starborne.Starborne;

public class BiomeHandler {
    public static final Biome SPACE = new SpaceBiome();

    public static void onPreInit() {
        GameRegistry.register(SPACE, new ResourceLocation(Starborne.MODID, "space"));
    }
}
