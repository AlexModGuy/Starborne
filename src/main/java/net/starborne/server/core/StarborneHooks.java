package net.starborne.server.core;

import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
            Vec3d transformedVelocity = transforming.getEntity().getTransformedVector(new Vec3d(particle.motionX, particle.motionY, particle.motionZ));
            particle.motionX = transformedVelocity.xCoord;
            particle.motionY = transformedVelocity.yCoord;
            particle.motionZ = transformedVelocity.zCoord;
        }
    }

    public static World getRealWorld(World world) {
        if (world instanceof StructureWorld) {
            return ((StructureWorld) world).getMainWorld();
        }
        return world;
    }
}
