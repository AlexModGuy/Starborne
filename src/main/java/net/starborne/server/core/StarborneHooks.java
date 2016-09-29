package net.starborne.server.core;

import net.minecraft.client.particle.Particle;
import net.starborne.server.entity.structure.world.StructureWorld;

import javax.vecmath.Point3d;

public class StarborneHooks {
    public static void transformEffect(Particle particle) {
        StructureWorld transforming = StructureWorld.transforming;
        if (transforming != null) {
            Point3d transformed = transforming.getEntity().getTransformedPosition(new Point3d(particle.posX, particle.posY, particle.posZ));
            particle.setPosition(transformed.getX(), transformed.getY(), transformed.getZ());
            particle.prevPosX = transformed.getX();
            particle.prevPosY = transformed.getY();
            particle.prevPosZ = transformed.getZ();
        }
    }
}
