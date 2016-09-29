package net.starborne.server.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.Name("starborne")
@IFMLLoadingPlugin.MCVersion("1.10.2")
@IFMLLoadingPlugin.SortingIndex(1002)
@IFMLLoadingPlugin.TransformerExclusions("net.ilexiconn.llibrary.server.asm")
public class StarbornePlugin implements IFMLLoadingPlugin {
    public static boolean loaded;
    public static boolean development;

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "net.starborne.server.patcher.StarborneRuntimePatcher", "net.starborne.server.core.StarborneTransformer" };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        StarbornePlugin.loaded = true;
        StarbornePlugin.development = !(Boolean) data.get("runtimeDeobfuscationEnabled");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
