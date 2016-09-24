package net.starborne.server.entity.structure.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.starborne.server.entity.structure.StructureEntity;

import javax.vecmath.Point3d;
import java.util.Random;

public class StructureWorldClient extends StructureWorld {
    private Minecraft mc = Minecraft.getMinecraft();

    public StructureWorldClient(StructureEntity entity) {
        super(entity);
        this.addEventListener(new ClientWorldListener());
    }

    @Override
    public void tick() {
        super.tick();
        EntityPlayerSP player = this.mc.thePlayer;
        if (player != null) {
            Point3d untransformedPosition = this.entity.getUntransformedPosition(new Point3d(player.posX, player.posY, player.posZ));
            this.runDisplayTicks((int) untransformedPosition.x, (int) untransformedPosition.y, (int) untransformedPosition.z);
        }
    }

    public void runDisplayTicks(int posX, int posY, int posZ) {
        Random random = new Random();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 667; i++) {
            this.runDisplayTicksArea(posX, posY, posZ, 16, random, pos);
            this.runDisplayTicksArea(posX, posY, posZ, 32, random, pos);
        }
    }

    public void runDisplayTicksArea(int posX, int posY, int posZ, int range, Random random, BlockPos.MutableBlockPos pos) {
        int x = posX + this.rand.nextInt(range) - this.rand.nextInt(range);
        int y = posY + this.rand.nextInt(range) - this.rand.nextInt(range);
        int z = posZ + this.rand.nextInt(range) - this.rand.nextInt(range);
        pos.setPos(x, y, z);
        IBlockState state = this.getBlockState(pos);
        state.getBlock().randomDisplayTick(state, this, pos, random);
    }
}
