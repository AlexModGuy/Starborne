package net.starborne.server.entity.structure;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.starborne.server.world.data.StarborneWorldSavedData;

import java.util.Random;

public class ChunkPartionHandler {
    public static BlockPos generateValidPartionPosition(World world) {
        StarborneWorldSavedData data = StarborneWorldSavedData.get(world);
        int attempts = 0;
        while (attempts < 100) {
            BlockPos position = ChunkPartionHandler.generatePartionPosition();
            if (!data.isOccupied(position)) {
                data.occupyChunk(position);
                return position;
            }
            attempts++;
        }
        return null;
    }

    public static BlockPos generatePartionPosition() {
        int size = 1875000;
        Random random = new Random();
        int x = random.nextInt(size * 2) - size;
        int z = (random.nextBoolean() ? 1 : -1) * size;
        if (x > size - 1) {
            x = size - 1;
        }
        if (z > size - 1) {
            z = size - 1;
        }
        int y = random.nextInt(16);
        if (random.nextBoolean()) {
            return new BlockPos(x, y, z);
        } else {
            return new BlockPos(z, y, x);
        }
    }
}
