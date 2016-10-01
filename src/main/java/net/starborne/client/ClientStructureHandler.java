package net.starborne.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.starborne.server.ServerStructureHandler;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.StructurePlayerHandler;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

public class ClientStructureHandler extends ServerStructureHandler {
    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();

    private StructureEntity mousedOver;
    private Queue<StructureEntity> queuedPlayerHandlers = new ArrayDeque<>();

    @Override
    public void update(World world) {
        super.update(world);

        EntityPlayerSP player = MINECRAFT.thePlayer;

        if (player != null) {
            while (this.queuedPlayerHandlers.size() > 0) {
                this.queuedPlayerHandlers.poll().addPlayerHandler(MINECRAFT.thePlayer);
            }

            Map.Entry<StructureEntity, RayTraceResult> mouseOver = this.getSelectedBlock(player);

            StructureEntity currentMousedOver = mouseOver != null ? mouseOver.getKey() : null;

            if (mouseOver != null) {
                StructurePlayerHandler mouseOverHandler = this.get(mouseOver.getKey(), player);
                if (mouseOverHandler != null) {
                    RayTraceResult prevMouseOver = mouseOverHandler.getMouseOver();
                    mouseOverHandler.setMouseOver(mouseOver.getValue());
                    if (prevMouseOver == null || !prevMouseOver.getBlockPos().equals(mouseOver.getValue().getBlockPos())) {
                        mouseOverHandler.startBreaking(null);
                    }
                }
            }

            if (this.mousedOver != null && this.mousedOver != currentMousedOver) {
                StructurePlayerHandler mouseOverHandler = this.get(this.mousedOver, player);
                if (mouseOverHandler != null) {
                    mouseOverHandler.startBreaking(null);
                    mouseOverHandler.setMouseOver(null);
                }
            }

            this.mousedOver = currentMousedOver;

            if (this.mousedOver != null && !MINECRAFT.gameSettings.keyBindAttack.isKeyDown()) {
                if (this.mousedOver != null) {
                    StructurePlayerHandler mouseOverHandler = this.get(this.mousedOver, player);
                    if (mouseOverHandler != null) {
                        mouseOverHandler.startBreaking(null);
                    }
                }
            }

            if (this.mousedOver != null && MINECRAFT.gameSettings.keyBindPickBlock.isKeyDown()) {
                StructurePlayerHandler handler = this.get(this.mousedOver, player);
                if (handler != null) {
                    handler.onPickBlock();
                }
            }
        }
    }

    @Override
    public StructureEntity getMousedOver(EntityPlayer player) {
        return this.mousedOver;
    }

    @Override
    public void addEntity(StructureEntity entity) {
        super.addEntity(entity);
        this.queuedPlayerHandlers.add(entity);
    }

    @Override
    public boolean isServer() {
        return false;
    }
}
