package net.starborne.server.patcher;

import net.ilexiconn.llibrary.server.asm.RuntimePatcher;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.starborne.server.core.StarborneHooks;

public class StarborneRuntimePatcher extends RuntimePatcher {
    @Override
    public void onInit() {
        this.patchClass(ParticleManager.class)
                .patchMethod("addEffect", Particle.class, void.class)
                    .apply(Patch.AFTER, data -> data.node.getOpcode() == POP, method -> method
                            .var(ALOAD, 1)
                            .method(INVOKESTATIC, StarborneHooks.class, "transformEffect", Particle.class, void.class));
    }
}
