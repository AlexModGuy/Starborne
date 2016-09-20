package net.starborne.server.patcher;

import net.ilexiconn.llibrary.server.asm.RuntimePatcher;

public class StarborneRuntimePatcher extends RuntimePatcher {
    @Override
    public void onInit() {
//        this.patchClass(DimensionManager.class)
//                .patchMethod("createProviderFor", int.class, WorldProvider.class)
//                    .apply(Patch.BEFORE, predicateData -> predicateData.node instanceof LabelNode && ((LabelNode) predicateData.node).getLabel().getOffset() == 6, (method) -> {
//                        method.var(ALOAD, 1).var(ILOAD, 1).method(INVOKESTATIC, StarborneHooks.class, "createProvider", WorldProvider.class, int.class, void.class);
//                    });
    }
}
