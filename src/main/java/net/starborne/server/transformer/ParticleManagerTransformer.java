package net.starborne.server.transformer;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.starborne.server.core.StarborneHooks;
import net.starborne.server.core.transformer.TransformerClass;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ParticleManagerTransformer extends TransformerClass {
    public ParticleManagerTransformer() {
        super(ParticleManager.class);
    }

    @Override
    public boolean transform(ClassNode classNode, String transformedName) {
        MethodNode addEffect = this.getMethod(classNode, "addEffect", Particle.class, void.class);
        if (addEffect != null) {
            Instruction instructions = this.instruction()
                    .var(ALOAD, 1)
                    .method(INVOKESTATIC, StarborneHooks.class, "transformEffect", Particle.class, void.class);
            this.insertAfter(addEffect, node -> node.getOpcode() == POP, instructions.build(), false);
            return true;
        }
        return false;
    }
}
