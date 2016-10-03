package net.starborne.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.starborne.Starborne;
import net.starborne.server.entity.structure.StructureEntity;
import net.starborne.server.entity.structure.StructurePlayerHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerStructureHandler {
    //TODO Multiple instances when there are multiple worlds?

    protected List<StructureEntity> structures = new ArrayList<>();
    protected Map<EntityPlayer, Map.Entry<StructureEntity, RayTraceResult>> mouseOver = new HashMap<>();

    public void update(World world) {
        List<StructureEntity> dead = new ArrayList<>();
        for (StructureEntity entity : this.structures) {
            if (entity.isDead) {
                dead.add(entity);
            }
        }
        this.structures.removeAll(dead);
        if (this.isServer()) {
            this.mouseOver.clear();
            for (EntityPlayer player : world.playerEntities) {
                this.mouseOver.put(player, this.getSelectedBlock(player));
            }
        }
    }

    public boolean onItemRightClick(EntityPlayer player, EnumHand hand) {
        boolean success = false;
        success |= this.interact(this.get(this.getMousedOver(player), player), player, hand);
        ItemStack heldItem = player.getHeldItem(hand);
        if (heldItem != null) {
            int prevSize = heldItem.stackSize;
            ActionResult<ItemStack> result = heldItem.useItemRightClick(player.worldObj, player, hand);
            if (result.getType() != EnumActionResult.SUCCESS) {
                for (StructureEntity entity : Starborne.PROXY.getStructureHandler(player.worldObj).getStructures()) {
                    result = heldItem.useItemRightClick(entity.structureWorld, player, hand);
                    if (result.getType() == EnumActionResult.SUCCESS) {
                        break;
                    }
                }
            }
            success |= result.getType() == EnumActionResult.SUCCESS;
            ItemStack output = result.getResult();
            if (output != heldItem || output.stackSize != prevSize) {
                if (player.capabilities.isCreativeMode && output.stackSize < prevSize) {
                    output.stackSize = prevSize;
                }
                player.setHeldItem(hand, output);
                if (output.stackSize <= 0) {
                    player.setHeldItem(hand, null);
                    ForgeEventFactory.onPlayerDestroyItem(player, output, hand);
                }
            }
        }
        return success;
    }

    public boolean interact(StructurePlayerHandler handler, EntityPlayer player, EnumHand hand) {
        if (handler != null) {
            RayTraceResult mouseOver = handler.getMouseOver();
            if (mouseOver != null && mouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
                return handler.interact(hand);
            }
        }
        return false;
    }

    public Map.Entry<StructureEntity, RayTraceResult> getSelectedBlock(EntityPlayer player) {
        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;
        double x = player.posX;
        double y = player.posY + player.getEyeHeight();
        double z = player.posZ;
        Vec3d start = new Vec3d(x, y, z);
        float pitchHorizontalFactor = -MathHelper.cos(-pitch * 0.017453292F);
        float deltaY = MathHelper.sin(-pitch * 0.017453292F);
        float deltaX = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI) * pitchHorizontalFactor;
        float deltaZ = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI) * pitchHorizontalFactor;
        double reach = 5.0;
        if (player instanceof EntityPlayerMP) {
            reach = ((EntityPlayerMP) player).interactionManager.getBlockReachDistance();
        }
        Vec3d end = start.addVector(deltaX * reach, deltaY * reach, deltaZ * reach);
        Map<StructureEntity, RayTraceResult> results = new HashMap<>();
        for (Entity entity : player.worldObj.loadedEntityList) {
            if (entity instanceof StructureEntity) {
                StructureEntity structure = (StructureEntity) entity;
                RayTraceResult result = structure.structureWorld.rayTraceBlocks(start, end);
                if (result != null && result.typeOfHit != RayTraceResult.Type.MISS) {
                    results.put(structure, result);
                }
            }
        }
        if (results.size() > 0) {
            Map.Entry<StructureEntity, RayTraceResult> closest = null;
            double closestDistance = Double.MAX_VALUE;
            for (Map.Entry<StructureEntity, RayTraceResult> entry : results.entrySet()) {
                RayTraceResult result = entry.getValue();
                double distance = result.hitVec.distanceTo(entry.getKey().getTransformedPosition(start));
                if (distance < closestDistance) {
                    closest = entry;
                    closestDistance = distance;
                }
            }
            return closest;
        }
        return null;
    }

    public void addEntity(StructureEntity entity) {
        this.structures.add(entity);
    }

    public void unloadWorld() {
        this.structures.clear();
    }

    public void removeEntity(StructureEntity entity) {
        this.structures.remove(entity);
    }

    public List<StructureEntity> getStructures() {
        return this.structures;
    }

    public StructureEntity getMousedOver(EntityPlayer player) {
        Map.Entry<StructureEntity, RayTraceResult> mouseOver = this.mouseOver.get(player);
        return mouseOver != null ? mouseOver.getKey() : null;
    }

    public StructurePlayerHandler get(StructureEntity entity, EntityPlayer player) {
        return entity != null ? entity.getPlayerHandlers().get(player) : null;
    }

    public void addPlayer(EntityPlayer player) {
    }

    public void removePlayer(EntityPlayer player) {
    }

    public boolean isServer() {
        return true;
    }
}
