package net.starborne.server.patcher;

import net.ilexiconn.llibrary.server.asm.RuntimePatcher;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.starborne.server.core.StarborneHooks;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class StarborneRuntimePatcher extends RuntimePatcher {
    @Override
    public void onInit() {
        this.patchClass(ParticleManager.class)
                .patchMethod("addEffect", Particle.class, void.class)
                .apply(Patch.AFTER, data -> data.node.getOpcode() == POP, method -> method
                        .var(ALOAD, 1)
                        .method(INVOKESTATIC, StarborneHooks.class, "transformEffect", Particle.class, void.class));
        this.patchClass(Entity.class)
                .patchMethod("<init>", World.class, void.class)
                .apply(Patch.AFTER, data -> data.node.getOpcode() == INVOKESPECIAL && ((MethodInsnNode) data.node).owner.equals("java/lang/Object"), method -> method
                        .var(ALOAD, 1)
                        .method(INVOKESTATIC, StarborneHooks.class, "getRealWorld", World.class, World.class)
                        .var(ASTORE, 1));
        this.patchClass(EntityList.class)
                .patchMethod("createEntityByName", String.class, World.class, Entity.class)
                    .apply(Patch.BEFORE, data -> data.node.getOpcode() == ASTORE && ((VarInsnNode) data.node).var == 2, method -> method
                        .var(ALOAD, 1)
                        .method(INVOKESTATIC, StarborneHooks.class, "getRealWorld", World.class, World.class)
                        .var(ASTORE, 1)).pop()
                .patchMethod("createEntityById", int.class, World.class, Entity.class)
                    .apply(Patch.BEFORE, data -> data.node.getOpcode() == ASTORE && ((VarInsnNode) data.node).var == 2, method -> method
                        .var(ALOAD, 1)
                        .method(INVOKESTATIC, StarborneHooks.class, "getRealWorld", World.class, World.class)
                        .var(ASTORE, 1)).pop()
                .patchMethod("createEntityFromNBT", NBTTagCompound.class, World.class, Entity.class)
                    .apply(Patch.BEFORE, data -> data.node.getOpcode() == ASTORE && ((VarInsnNode) data.node).var == 2, method -> method
                        .var(ALOAD, 1)
                        .method(INVOKESTATIC, StarborneHooks.class, "getRealWorld", World.class, World.class)
                        .var(ASTORE, 1));
    }
}
