package net.starborne.server.entity.structure;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraftforge.event.ForgeEventFactory;
import net.starborne.Starborne;
import net.starborne.server.entity.structure.world.StructureWorld;
import net.starborne.server.message.BreakBlockEntityMessage;
import net.starborne.server.message.EntityChunkMessage;
import net.starborne.server.message.InteractBlockEntityMessage;
import net.starborne.server.message.SetEntityBlockMessage;

import javax.vecmath.Point3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StructurePlayerHandler {
    private final StructureEntity entity;
    private final EntityPlayer player;
    private List<EntityChunk> dirty = new LinkedList<>();
    private Map<EntityChunk, BlockStateContainer> lastData = new HashMap<>();

    private BlockPos breaking;
    private boolean isHittingBlock;

    private float breakProgress;
    private int interactCooldown;
    private int breakDelay;
    private RayTraceResult mouseOver;

    private float breakSoundTimer;

    public StructurePlayerHandler(StructureEntity entity, EntityPlayer player) {
        this.entity = entity;
        this.player = player;
    }

    public void update() {
        if (this.interactCooldown > 0) {
            this.interactCooldown--;
        }
        if (this.breakDelay > 0) {
            this.breakDelay--;
        }
        if (this.breaking != null) {
            if (this.updateBreaking()) {
                this.player.swingArm(EnumHand.MAIN_HAND);
            }
        }
        if (!this.player.worldObj.isRemote && this.player instanceof EntityPlayerMP) {
            if (this.dirty.size() > 0) {
                EntityChunk chunk = this.dirty.get(0);
                BlockStateContainer newLast = new BlockStateContainer();
                BlockStateContainer difference = new BlockStateContainer();
                BlockStateContainer last = this.lastData.get(chunk);
                List<BlockPos> differences = new ArrayList<>();
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            IBlockState state = chunk.getBlockState(x, y, z);
                            newLast.set(x, y, z, state);
                            if (last == null || last.get(x, y, z) != state) {
                                difference.set(x, y, z, state);
                                differences.add(new BlockPos(x, y, z));
                            }
                        }
                    }
                }
                if (differences.size() > 0) {
                    if (differences.size() == 1) {
                        BlockPos chunkPos = chunk.getPosition();
                        BlockPos globalPosition = differences.get(0).add(chunkPos.getX() << 4, chunkPos.getY() << 4, chunkPos.getZ() << 4);
                        Starborne.networkWrapper.sendTo(new SetEntityBlockMessage(this.entity.getEntityId(), globalPosition, chunk.getBlockState(differences.get(0))), (EntityPlayerMP) this.player);
                    } else {
                        Starborne.networkWrapper.sendTo(new EntityChunkMessage(this.entity.getEntityId(), chunk), (EntityPlayerMP) this.player);
                    }
                }
                this.lastData.put(chunk, newLast);
                this.dirty.remove(0);
            }
        }
    }

    public void setDirty(EntityChunk chunk) {
        if (!this.dirty.contains(chunk)) {
            this.dirty.add(chunk);
        }
    }

    public void remove(EntityChunk chunk) {
        this.lastData.remove(chunk);
    }

    public boolean clickBlock(BlockPos pos) {
        if (this.interactCooldown <= 0) {
            this.interactCooldown = 3;
            if (this.player.capabilities.isCreativeMode) {
                this.breakBlock(pos);
            } else if (!this.isHittingBlock || this.breaking == null) {
                Starborne.networkWrapper.sendToServer(new BreakBlockEntityMessage(this.entity.getEntityId(), pos, BreakBlockEntityMessage.BreakState.START));
                IBlockState state = this.entity.getBlockState(pos);
                boolean realBlock = state.getMaterial() != Material.AIR;
                if (realBlock && this.breakProgress == 0.0F) {
                    state.getBlock().onBlockClicked(this.entity.structureWorld, pos, this.player);
                }
                if (realBlock && state.getPlayerRelativeBlockHardness(this.player, this.entity.structureWorld, pos) >= 1.0F) {
                    this.breakBlock(pos);
                } else {
                    this.isHittingBlock = true;
                    this.breaking = pos;
                    this.breakProgress = 0.0F;
                    this.breakSoundTimer = 0.0F;
                }
            }
            return true;
        }
        return false;
    }

    public boolean updateBreaking() {
        if (this.breakDelay <= 0 && this.breaking != null && this.isHittingBlock) {
            this.breakDelay = 2;
            IBlockState state = this.entity.structureWorld.getBlockState(this.breaking);
            Block block = state.getBlock();
            if (state.getMaterial() == Material.AIR) {
                this.isHittingBlock = false;
                return false;
            } else {
                this.breakProgress += state.getPlayerRelativeBlockHardness(this.player, this.entity.structureWorld, this.breaking);
                if (this.breakSoundTimer % 4.0F == 0.0F) {
                    SoundType soundType = block.getSoundType();
                    Point3d point = this.entity.getTransformedPosition(new Point3d(this.breaking.getX(), this.breaking.getY(), this.breaking.getZ()));
                    Starborne.PROXY.playSound(new PositionedSoundRecord(soundType.getHitSound(), SoundCategory.NEUTRAL, (soundType.getVolume() + 1.0F) / 8.0F, soundType.getPitch() * 0.5F, new BlockPos(point.getX(), point.getY(), point.getZ())));
                }
                this.breakSoundTimer++;
                if (this.breakProgress >= 1.0F) {
                    this.isHittingBlock = false;
                    Starborne.networkWrapper.sendToServer(new BreakBlockEntityMessage(this.entity.getEntityId(), this.breaking, BreakBlockEntityMessage.BreakState.STOP));
                    this.breakBlock(this.breaking);
                    this.breakProgress = 0.0F;
                    this.breakSoundTimer = 0.0F;
                    this.interactCooldown = 5;
                }
                return true;
            }
        }
        return false;
    }

    public void breakBlock(BlockPos pos) {
        IBlockState state = this.entity.getBlockState(pos);
        Block block = state.getBlock();
        this.entity.structureWorld.playEvent(2001, pos, Block.getStateId(state));
        this.entity.setBlockState(pos, Blocks.AIR.getDefaultState());
        Starborne.networkWrapper.sendToServer(new BreakBlockEntityMessage(this.entity.getEntityId(), pos, BreakBlockEntityMessage.BreakState.BREAK));
        if (!this.player.capabilities.isCreativeMode) {
            ItemStack heldItem = this.player.getHeldItemMainhand();
            if (heldItem != null) {
                heldItem.onBlockDestroyed(this.entity.structureWorld, state, pos, this.player);
                if (heldItem.stackSize <= 0) {
                    ForgeEventFactory.onPlayerDestroyItem(this.player, heldItem, EnumHand.MAIN_HAND);
                    this.player.setHeldItem(EnumHand.MAIN_HAND, null);
                }
            }
        }
        boolean removed = block.removedByPlayer(state, this.entity.structureWorld, pos, this.player, false);
        if (removed) {
            block.onBlockDestroyedByPlayer(this.entity.structureWorld, pos, state);
        }
    }

    public boolean interact(EnumHand hand) {
        if (this.interactCooldown <= 0) {
            this.interactCooldown = 3;
            BlockPos pos = this.mouseOver.getBlockPos();
            Vec3d hitVec = this.mouseOver.hitVec;
            IBlockState state = this.entity.getBlockState(pos);
            float hitX = (float) (hitVec.xCoord - pos.getX());
            float hitY = (float) (hitVec.yCoord - pos.getY());
            float hitZ = (float) (hitVec.zCoord - pos.getZ());
            ItemStack heldItem = this.player.getHeldItem(hand);
            Starborne.networkWrapper.sendToServer(new InteractBlockEntityMessage(this.entity.getEntityId(), pos, this.mouseOver.sideHit, hitX, hitY, hitZ, hand));
            if (state.getBlock().onBlockActivated(this.entity.structureWorld, pos, state, this.player, hand, heldItem, this.mouseOver.sideHit, hitX, hitY, hitZ)) {
                this.player.swingArm(hand);
                return true;
            } else if (heldItem != null) {
                int size = heldItem.stackSize;
                EnumActionResult actionResult = heldItem.onItemUse(this.player, this.entity.structureWorld, pos, hand, this.mouseOver.sideHit, hitX, hitY, hitZ);
                if (actionResult == EnumActionResult.SUCCESS) {
                    this.player.swingArm(hand);
                }
                if (this.player.capabilities.isCreativeMode && heldItem.stackSize < size) {
                    heldItem.stackSize = size;
                }
                if (heldItem.stackSize <= 0) {
                    this.player.setHeldItem(hand, null);
                    ForgeEventFactory.onPlayerDestroyItem(this.player, heldItem, hand);
                }
                return true;
            }
        }
        return false;
    }

    public RayTraceResult getMouseOver() {
        return this.mouseOver;
    }

    public void setMouseOver(RayTraceResult mouseOver) {
        this.mouseOver = mouseOver;
    }

    public BlockPos getBreaking() {
        return this.breaking;
    }

    public float getBreakProgress() {
        return this.breakProgress;
    }

    public void startBreaking(BlockPos position) {
        this.breaking = position;
        this.breakProgress = 0.0F;
    }

    public boolean onPickBlock() {
        TileEntity tile = null;
        if (this.mouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
            StructureWorld structureWorld = this.entity.structureWorld;
            IBlockState state = structureWorld.getBlockState(this.mouseOver.getBlockPos());
            if (!state.getBlock().isAir(state, structureWorld, this.mouseOver.getBlockPos())) {
                Starborne.PROXY.pickBlock(this.player, this.mouseOver, tile, structureWorld, state);
            }
        }
        return false;
    }
}
