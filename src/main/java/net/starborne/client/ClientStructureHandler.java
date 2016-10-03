package net.starborne.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.starborne.server.ServerStructureHandler;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.StructurePlayerHandler;

import java.util.Map;

public class ClientStructureHandler extends ServerStructureHandler {
    private static final Minecraft MINECRAFT = Minecraft.getMinecraft();

    private StructureEntity mousedOver;

    @Override
    public void update(World world) {
        super.update(world);

        EntityPlayerSP clientPlayer = MINECRAFT.thePlayer;

        if (clientPlayer != null) {
            Map.Entry<StructureEntity, RayTraceResult> mouseOver = this.getSelectedBlock(clientPlayer);

            StructureEntity currentMousedOver = mouseOver != null ? mouseOver.getKey() : null;

            if (mouseOver != null) {
                StructurePlayerHandler mouseOverHandler = this.get(mouseOver.getKey(), clientPlayer);
                if (mouseOverHandler != null) {
                    RayTraceResult prevMouseOver = mouseOverHandler.getMouseOver();
                    mouseOverHandler.setMouseOver(mouseOver.getValue());
                    if (prevMouseOver == null || !prevMouseOver.getBlockPos().equals(mouseOver.getValue().getBlockPos())) {
                        mouseOverHandler.startBreaking(null);
                    }
                }
            }

            if (this.mousedOver != null && this.mousedOver != currentMousedOver) {
                StructurePlayerHandler mouseOverHandler = this.get(this.mousedOver, clientPlayer);
                if (mouseOverHandler != null) {
                    mouseOverHandler.startBreaking(null);
                    mouseOverHandler.setMouseOver(null);
                }
            }

            this.mousedOver = currentMousedOver;

            if (this.mousedOver != null && !MINECRAFT.gameSettings.keyBindAttack.isKeyDown()) {
                if (this.mousedOver != null) {
                    StructurePlayerHandler mouseOverHandler = this.get(this.mousedOver, clientPlayer);
                    if (mouseOverHandler != null) {
                        mouseOverHandler.startBreaking(null);
                    }
                }
            }

            if (this.mousedOver != null && MINECRAFT.gameSettings.keyBindPickBlock.isKeyDown()) {
                StructurePlayerHandler handler = this.get(this.mousedOver, clientPlayer);
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
    public void addPlayer(EntityPlayer player) {
        for (StructureEntity structure : this.structures) {
            structure.addPlayerHandler(player);
        }
    }

    @Override
    public void removePlayer(EntityPlayer player) {
        for (StructureEntity structure : this.structures) {
            structure.removePlayerHandler(player);
        }
    }

    @Override
    public void addEntity(StructureEntity entity) {
        super.addEntity(entity);
        for (EntityPlayer player : entity.worldObj.playerEntities) {
            entity.addPlayerHandler(player);
        }
    }

    @Override
    public boolean isServer() {
        return false;
    }
}
