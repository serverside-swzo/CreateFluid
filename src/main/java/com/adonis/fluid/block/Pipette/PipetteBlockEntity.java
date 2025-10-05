package com.adonis.fluid.block.Pipette;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.content.pipette.DepotFluidInteractionPoint;
import com.adonis.fluid.content.pipette.FluidInteractionPoint;
import com.adonis.fluid.handler.CFPacketHandler;
import com.adonis.fluid.packet.PipetteParticlePacket;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PipetteBlockEntity extends KineticBlockEntity
        implements TransformableBlockEntity, IHaveGoggleInformation {

    // 字段定义
    public List<FluidInteractionPoint> inputs = new ArrayList<>();
    public List<FluidInteractionPoint> outputs = new ArrayList<>();
    public ListTag interactionPointTag = null;
    float chasedPointProgress;
    int chasedPointIndex;
    public FluidStack heldFluid;
    Phase phase;
    public boolean goggles;
    PipetteAngleTarget previousTarget;
    public LerpedFloat lowerArmAngle;
    public LerpedFloat upperArmAngle;
    public LerpedFloat baseAngle;
    public LerpedFloat headAngle;
    public LerpedFloat clawAngle;
    float previousBaseAngle;
    boolean updateInteractionPoints;
    int tooltipWarmup;
    protected ScrollOptionBehaviour<SelectionMode> selectionMode;
    protected int lastInputIndex = -1;
    protected int lastOutputIndex = -1;
    protected boolean redstoneLocked;
    private float previousProgressForInjection = 0.0F;

    // 流体相关常量
    private static final int TRANSFER_AMOUNT = 1000;
    private static final int FLUID_CAPACITY = 1000;

    public PipetteBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.heldFluid = FluidStack.EMPTY;
        this.phase = Phase.SEARCH_INPUTS;
        this.previousTarget = PipetteAngleTarget.NO_TARGET;
        this.baseAngle = LerpedFloat.angular();
        this.baseAngle.startWithValue(this.previousTarget.baseAngle);
        this.lowerArmAngle = LerpedFloat.angular();
        this.lowerArmAngle.startWithValue(this.previousTarget.lowerArmAngle);
        this.upperArmAngle = LerpedFloat.angular();
        this.upperArmAngle.startWithValue(this.previousTarget.upperArmAngle);
        this.headAngle = LerpedFloat.angular();
        this.headAngle.startWithValue(this.previousTarget.headAngle);
        this.clawAngle = LerpedFloat.angular();
        this.previousBaseAngle = this.previousTarget.baseAngle;
        this.updateInteractionPoints = true;
        this.redstoneLocked = false;
        this.tooltipWarmup = 15;
        this.goggles = false;
    }

    public int getFluidCapacity() {
        return FLUID_CAPACITY;
    }

    public FluidStack getHeldFluid() {
        return this.heldFluid.copy();
    }

    public boolean hasFluid() {
        return !this.heldFluid.isEmpty();
    }

    public float getFluidFillRatio() {
        if (this.heldFluid.isEmpty()) return 0.0f;
        return (float) this.heldFluid.getAmount() / FLUID_CAPACITY;
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        this.selectionMode = new ScrollOptionBehaviour<>(SelectionMode.class,
                CreateLang.translateDirect("logistics.when_multiple_outputs_available"),
                this, new SelectionModeValueBox());
        behaviours.add(this.selectionMode);
    }

    @Override
    public void tick() {
        super.tick();

        this.initInteractionPoints();
        boolean targetReached = this.tickMovementProgress();

        if (this.tooltipWarmup > 0) {
            --this.tooltipWarmup;
        }

        if (this.chasedPointProgress < 1.0F) {
            if (this.phase == Phase.MOVE_TO_INPUT) {
                FluidInteractionPoint point = this.getTargetedInteractionPoint();
                if (point != null) {
                    point.keepAlive();
                }
            }
        } else if (!this.level.isClientSide) {
            if (this.phase == Phase.MOVE_TO_INPUT) {
                this.collectFluid();
            } else if (this.phase == Phase.MOVE_TO_OUTPUT) {
                this.depositFluid();
            } else if (this.phase == Phase.SEARCH_INPUTS) {
                this.searchForFluid();
            }

            if (targetReached) {
                this.lazyTick();
            }
        }

        previousProgressForInjection = this.chasedPointProgress;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!this.level.isClientSide) {
            if (this.chasedPointProgress >= 0.5F && this.phase == Phase.SEARCH_OUTPUTS) {
                this.searchForDestination();
            }
        }
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(3.0);
    }

    private boolean tickMovementProgress() {
        boolean targetReachedPreviously = this.chasedPointProgress >= 1.0F;

        float speed = Math.abs(this.getSpeed());
        float increment = Math.min(256.0F, speed) / 1024.0F;

        if (speed >= 256.0F && increment > 0.2F) {
            increment = 0.2F;
        }

        this.chasedPointProgress += increment;

        if (this.chasedPointProgress > 1.0F) {
            this.chasedPointProgress = 1.0F;
        }

        if (this.level.isClientSide) {
            FluidInteractionPoint targetedInteractionPoint = this.getTargetedInteractionPoint();
            PipetteAngleTarget previousTarget = this.previousTarget;
            PipetteAngleTarget target = targetedInteractionPoint == null ? PipetteAngleTarget.NO_TARGET :
                    this.createAngleTarget(targetedInteractionPoint);

            double currentBaseAngle = AngleHelper.angleLerp(this.chasedPointProgress, this.previousBaseAngle,
                    target == PipetteAngleTarget.NO_TARGET ? this.previousBaseAngle : target.baseAngle);
            this.baseAngle.setValue(currentBaseAngle);

            if (this.chasedPointProgress < 0.5F) {
                target = PipetteAngleTarget.NO_TARGET;
            } else {
                previousTarget = PipetteAngleTarget.NO_TARGET;
            }

            float progress = this.chasedPointProgress == 1.0F ? 1.0F :
                    this.chasedPointProgress % 0.5F * 2.0F;

            double lowerAngle = Mth.lerp(progress, previousTarget.lowerArmAngle, target.lowerArmAngle);
            double upperAngle = Mth.lerp(progress, previousTarget.upperArmAngle, target.upperArmAngle);
            double headAngleValue = AngleHelper.angleLerp(progress,
                    previousTarget.headAngle % 360.0F, target.headAngle % 360.0F);

            this.lowerArmAngle.setValue(lowerAngle);
            this.upperArmAngle.setValue(upperAngle);
            this.headAngle.setValue(headAngleValue);

            return false;
        } else {
            return !targetReachedPreviously && this.chasedPointProgress >= 1.0F;
        }
    }

    protected boolean isOnCeiling() {
        BlockState state = this.getBlockState();
        return this.hasLevel() && state.getOptionalValue(PipetteBlock.CEILING).orElse(false);
    }

    public void setInteractionPointTag(ListTag tag) {
        this.interactionPointTag = tag;
        this.updateInteractionPoints = true;
    }

    public void setUpdateInteractionPoints(boolean update) {
        this.updateInteractionPoints = update;
    }

    public boolean shouldUpdateInteractionPoints() {
        return this.updateInteractionPoints;
    }

    @Nullable
    private FluidInteractionPoint getTargetedInteractionPoint() {
        if (this.chasedPointIndex == -1) {
            return null;
        } else if (this.phase == Phase.MOVE_TO_INPUT && this.chasedPointIndex < this.inputs.size()) {
            return this.inputs.get(this.chasedPointIndex);
        } else {
            return this.phase == Phase.MOVE_TO_OUTPUT && this.chasedPointIndex < this.outputs.size() ?
                    this.outputs.get(this.chasedPointIndex) : null;
        }
    }

    private PipetteAngleTarget createAngleTarget(FluidInteractionPoint point) {
        return new PipetteAngleTarget(this.worldPosition, VecHelper.getCenterOf(point.getPos()),
                Direction.DOWN, this.isOnCeiling());
    }

    private boolean canFluidBeOutputted(FluidStack fluid) {
        if (fluid.isEmpty()) return false;

        for (FluidInteractionPoint output : this.outputs) {
            if (output.isValid() && output.canInsert(fluid)) {
                return true;
            }
        }
        return false;
    }

    protected void searchForFluid() {
        if (this.redstoneLocked) return;

        // 如果有流体，优先检查是否有置物台需要注液
        if (!this.heldFluid.isEmpty()) {
            for (FluidInteractionPoint output : this.outputs) {
                if (output instanceof DepotFluidInteractionPoint depotPoint) {
                    if (depotPoint.hasItemForFilling() && depotPoint.canInsert(this.heldFluid)) {
                        this.phase = Phase.SEARCH_OUTPUTS;
                        this.chasedPointProgress = 0.0F;
                        this.chasedPointIndex = -1;
                        searchForDestination();
                        return;
                    }
                } else if (output.isValid() && output.canInsert(this.heldFluid)) {
                    this.phase = Phase.SEARCH_OUTPUTS;
                    this.chasedPointProgress = 0.0F;
                    this.chasedPointIndex = -1;
                    searchForDestination();
                    return;
                }
            }
        }

        if (!this.heldFluid.isEmpty()) {
            for (FluidInteractionPoint output : this.outputs) {
                if (output.isValid() && output.canInsert(this.heldFluid)) {
                    this.phase = Phase.SEARCH_OUTPUTS;
                    this.chasedPointProgress = 0.0F;
                    this.chasedPointIndex = -1;
                    searchForDestination();
                    return;
                }
            }
        }

        boolean foundInput = false;
        int startIndex = this.selectionMode.get() == SelectionMode.PREFER_FIRST ? 0 : this.lastInputIndex + 1;
        int scanRange = this.selectionMode.get() == SelectionMode.FORCED_ROUND_ROBIN ?
                this.lastInputIndex + 2 : this.inputs.size();
        if (scanRange > this.inputs.size()) {
            scanRange = this.inputs.size();
        }

        for(int i = startIndex; i < scanRange; ++i) {
            FluidInteractionPoint point = this.inputs.get(i);
            if (point.isValid() && point.canExtract()) {
                FluidStack simulatedFluid = point.extract(TRANSFER_AMOUNT, true);

                if (!simulatedFluid.isEmpty() && canFluidBeOutputted(simulatedFluid)) {
                    this.selectIndex(true, i);
                    foundInput = true;
                    break;
                }
            }
        }

        if (!foundInput && this.selectionMode.get() == SelectionMode.ROUND_ROBIN) {
            this.lastInputIndex = -1;
        }

        if (this.lastInputIndex == this.inputs.size() - 1) {
            this.lastInputIndex = -1;
        }
    }

    protected void searchForDestination() {
        FluidStack held = this.heldFluid.copy();
        boolean foundOutput = false;

        for (int i = 0; i < this.outputs.size(); i++) {
            FluidInteractionPoint point = this.outputs.get(i);

            if (point.isValid() && point.canInsert(held)) {
                this.selectIndex(false, i);
                foundOutput = true;
                break;
            }
        }
    }

    private void selectIndex(boolean input, int index) {
        this.phase = input ? Phase.MOVE_TO_INPUT : Phase.MOVE_TO_OUTPUT;
        this.chasedPointIndex = index;
        this.chasedPointProgress = 0.0F;
        if (input) {
            this.lastInputIndex = index;
        } else {
            this.lastOutputIndex = index;
        }

        this.sendData();
        this.setChanged();
    }

    protected void depositFluid() {
        FluidInteractionPoint point = this.getTargetedInteractionPoint();
        if (point == null || !point.isValid()) {
            this.phase = this.heldFluid.isEmpty() ? Phase.SEARCH_INPUTS : Phase.SEARCH_OUTPUTS;
            this.chasedPointProgress = 0.0F;
            this.chasedPointIndex = -1;
            this.sendData();
            this.setChanged();
            return;
        }

        // 特殊处理置物台
        if (point instanceof DepotFluidInteractionPoint depotPoint) {
            DepotBehaviour behaviour = BlockEntityBehaviour.get(level, point.getPos(), DepotBehaviour.TYPE);
            if (behaviour == null) {
                return;
            }

            ItemStack itemOnDepot = behaviour.getHeldItemStack();

            if (itemOnDepot != null && !itemOnDepot.isEmpty()) {
                ItemStack singleItem = itemOnDepot.copy();
                singleItem.setCount(1);

                int requiredAmount = com.simibubi.create.content.fluids.spout.FillingBySpout
                        .getRequiredAmountForItem(this.level, singleItem, this.heldFluid);

                if (requiredAmount > 0 && requiredAmount <= this.heldFluid.getAmount()) {
                    // 重要：在调用 fillItem 之前保存流体副本！
                    FluidStack fluidForParticles = this.heldFluid.copy();
                    fluidForParticles.setAmount(requiredAmount);

                    FluidStack fluidForFilling = this.heldFluid.copy();
                    fluidForFilling.setAmount(requiredAmount);

                    ItemStack result = com.simibubi.create.content.fluids.spout.FillingBySpout
                            .fillItem(this.level, requiredAmount, singleItem, fluidForFilling);

                    if (!result.isEmpty()) {
                        // 只减少一个物品
                        itemOnDepot.shrink(1);

                        // 创建新的 TransportedItemStack 列表
                        List<com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack> allStacks = new ArrayList<>();

                        // 如果还有剩余的原物品，添加到列表
                        if (!itemOnDepot.isEmpty()) {
                            com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack remainingStack =
                                    new com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack(itemOnDepot);
                            remainingStack.beltPosition = 0.5f;
                            remainingStack.prevBeltPosition = 0.5f;
                            allStacks.add(remainingStack);
                        }

                        // 添加结果物品到列表
                        com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack resultStack =
                                new com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack(result);
                        resultStack.beltPosition = 0.5f;
                        resultStack.prevBeltPosition = 0.5f;
                        allStacks.add(resultStack);

                        // 设置置物台上的物品
                        if (!allStacks.isEmpty()) {
                            behaviour.setHeldItem(allStacks.get(0));

                            // 处理额外的物品
                            if (allStacks.size() > 1) {
                                try {
                                    java.lang.reflect.Field bufferField = DepotBehaviour.class.getDeclaredField("processingOutputBuffer");
                                    bufferField.setAccessible(true);
                                    net.neoforged.neoforge.items.ItemStackHandler outputBuffer =
                                            (net.neoforged.neoforge.items.ItemStackHandler) bufferField.get(behaviour);

                                    for (int i = 1; i < allStacks.size(); i++) {
                                        ItemStack stackToInsert = allStacks.get(i).stack;
                                        for (int slot = 0; slot < outputBuffer.getSlots() && !stackToInsert.isEmpty(); slot++) {
                                            stackToInsert = outputBuffer.insertItem(slot, stackToInsert, false);
                                        }

                                        if (!stackToInsert.isEmpty()) {
                                            net.minecraft.world.phys.Vec3 dropPos =
                                                    net.createmod.catnip.math.VecHelper.getCenterOf(point.getPos());
                                            net.minecraft.world.Containers.dropItemStack(
                                                    this.level,
                                                    dropPos.x,
                                                    dropPos.y + 0.5,
                                                    dropPos.z,
                                                    stackToInsert
                                            );
                                        }
                                    }
                                } catch (Exception e) {
                                    for (int i = 1; i < allStacks.size(); i++) {
                                        net.minecraft.world.phys.Vec3 dropPos =
                                                net.createmod.catnip.math.VecHelper.getCenterOf(point.getPos());
                                        net.minecraft.world.Containers.dropItemStack(
                                                this.level,
                                                dropPos.x,
                                                dropPos.y + 0.5,
                                                dropPos.z,
                                                allStacks.get(i).stack
                                        );
                                    }
                                }
                            }
                        }

                        behaviour.blockEntity.notifyUpdate();
                        this.heldFluid.shrink(requiredAmount);

                        // 播放音效
                        this.level.playSound(null, point.getPos(),
                                com.simibubi.create.AllSoundEvents.SPOUTING.getMainEvent(),
                                net.minecraft.sounds.SoundSource.BLOCKS,
                                0.75F, 0.9F + 0.2F * this.level.random.nextFloat());

                        // 发送粒子效果
                        if (!this.level.isClientSide && !fluidForParticles.isEmpty()) {
                            sendFillingParticles(point.getPos(), fluidForParticles);
                        }
                    }
                }
            }
        } else {
            FluidStack toInsert = this.heldFluid.copy();
            FluidStack remainder = point.insert(toInsert, false);
            this.heldFluid = remainder;
        }

        // 重置状态
        this.phase = this.heldFluid.isEmpty() ? Phase.SEARCH_INPUTS : Phase.SEARCH_OUTPUTS;
        this.chasedPointProgress = 0.0F;
        this.chasedPointIndex = -1;
        this.sendData();
        this.setChanged();
    }

    private void sendFillingParticles(BlockPos targetPos, FluidStack fluid) {
        if (this.level.isClientSide) {
            return;
        }

        if (fluid.isEmpty()) {
            return;
        }

        Vec3 particlePos = net.createmod.catnip.math.VecHelper.getCenterOf(targetPos).add(0, 0.8125, 0);

        PipetteParticlePacket packet = new PipetteParticlePacket(particlePos, fluid);

        try {
            CFPacketHandler.sendToPlayersTrackingChunk(
                    (ServerLevel) this.level,
                    new ChunkPos(targetPos),
                    packet
            );
        } catch (Exception e) {
            // 静默处理异常
        }
    }

    protected void collectFluid() {
        FluidInteractionPoint point = this.getTargetedInteractionPoint();
        if (point != null && point.isValid()) {
            int maxCanExtract = FLUID_CAPACITY;

            if (!this.heldFluid.isEmpty()) {
                maxCanExtract = FLUID_CAPACITY - this.heldFluid.getAmount();

                if (maxCanExtract <= 0) {
                    this.phase = Phase.SEARCH_OUTPUTS;
                    this.chasedPointProgress = 0.0F;
                    this.chasedPointIndex = -1;
                    this.sendData();
                    this.setChanged();
                    return;
                }
            }

            int extractAmount = Math.min(TRANSFER_AMOUNT, maxCanExtract);

            FluidStack extracted = point.extract(extractAmount, false);
            if (!extracted.isEmpty()) {
                // 使用 FluidStack.matches() 替代已弃用的 isFluidEqual()
                if (!this.heldFluid.isEmpty() && FluidStack.isSameFluidSameComponents(this.heldFluid, extracted)) {
                    this.heldFluid.grow(extracted.getAmount());
                } else if (this.heldFluid.isEmpty()) {
                    this.heldFluid = extracted;
                }

                this.phase = Phase.SEARCH_OUTPUTS;
                this.chasedPointProgress = 0.0F;
                this.chasedPointIndex = -1;
                this.sendData();
                this.setChanged();

                this.level.playSound(null, this.worldPosition, SoundEvents.BUCKET_FILL,
                        SoundSource.BLOCKS, 0.125F, 0.5F + this.level.random.nextFloat() * 0.25F);
                return;
            }
        }

        this.phase = Phase.SEARCH_INPUTS;
        this.chasedPointProgress = 0.0F;
        this.chasedPointIndex = -1;
        this.sendData();
        this.setChanged();
    }

    public void redstoneUpdate() {
        if (!this.level.isClientSide) {
            boolean blockPowered = this.level.hasNeighborSignal(this.worldPosition);
            if (blockPowered != this.redstoneLocked) {
                this.redstoneLocked = blockPowered;
                this.sendData();
                if (!this.redstoneLocked) {
                    this.searchForFluid();
                }
            }
        }
    }

    public void resetMovementState() {
        this.phase = Phase.SEARCH_INPUTS;
        this.chasedPointProgress = 0.0F;
        this.chasedPointIndex = -1;
    }

    public void forceInitInteractionPoints() {
        this.initInteractionPoints();
    }

    public void forceReloadInteractionPoints() {
        if (interactionPointTag != null && level != null) {
            inputs.clear();
            outputs.clear();

            for (Tag tag : interactionPointTag) {
                FluidInteractionPoint point = FluidInteractionPoint.deserialize(
                        (CompoundTag) tag, level, worldPosition);
                if (point != null) {
                    if (point.getMode() == FluidInteractionPoint.Mode.TAKE) {
                        inputs.add(point);
                    } else {
                        outputs.add(point);
                    }
                }
            }

            updateInteractionPoints = false;
            sendData();
            setChanged();
        }
    }

    @Override
    public void transform(BlockEntity be, StructureTransform transform) {
        if (this.interactionPointTag != null) {
            for (Tag tag : this.interactionPointTag) {
                FluidInteractionPoint.transformPos((CompoundTag)tag, transform);
            }
            this.notifyUpdate();
        }
    }

    protected boolean isAreaActuallyLoaded(BlockPos center, int range) {
        if (!this.level.isAreaLoaded(center, range)) {
            return false;
        } else {
            if (this.level.isClientSide) {
                int minY = center.getY() - range;
                int maxY = center.getY() + range;
                if (maxY < this.level.getMinBuildHeight() || minY >= this.level.getMaxBuildHeight()) {
                    return false;
                }

                int minX = center.getX() - range;
                int minZ = center.getZ() - range;
                int maxX = center.getX() + range;
                int maxZ = center.getZ() + range;
                int minChunkX = SectionPos.blockToSectionCoord(minX);
                int maxChunkX = SectionPos.blockToSectionCoord(maxX);
                int minChunkZ = SectionPos.blockToSectionCoord(minZ);
                int maxChunkZ = SectionPos.blockToSectionCoord(maxZ);
                ChunkSource chunkSource = this.level.getChunkSource();

                for(int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
                    for(int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
                        if (!chunkSource.hasChunk(chunkX, chunkZ)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }

    protected void initInteractionPoints() {
        if (this.updateInteractionPoints && this.interactionPointTag != null) {
            if (this.isAreaActuallyLoaded(this.worldPosition, getRange() + 1)) {
                this.inputs.clear();
                this.outputs.clear();

                for (Tag tag : this.interactionPointTag) {
                    FluidInteractionPoint point = FluidInteractionPoint.deserialize(
                            (CompoundTag)tag, this.level, this.worldPosition);
                    if (point != null) {
                        if (point.getMode() == FluidInteractionPoint.Mode.DEPOSIT) {
                            this.outputs.add(point);
                        } else if (point.getMode() == FluidInteractionPoint.Mode.TAKE) {
                            this.inputs.add(point);
                        }
                    }
                }

                this.updateInteractionPoints = false;
                this.sendData();
                this.setChanged();
            }
        }
    }

    public void writeInteractionPoints(CompoundTag compound) {
        if (this.updateInteractionPoints && this.interactionPointTag != null) {
            compound.put("InteractionPoints", this.interactionPointTag);
        } else {
            ListTag pointsNBT = new ListTag();
            this.inputs.stream().map(fip -> fip.serialize(this.worldPosition)).forEach(pointsNBT::add);
            this.outputs.stream().map(fip -> fip.serialize(this.worldPosition)).forEach(pointsNBT::add);
            compound.put("InteractionPoints", pointsNBT);
        }
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);

        // 写入交互点
        writeInteractionPoints(compound);

        NBTHelper.writeEnum(compound, "Phase", this.phase);
        compound.putBoolean("Powered", this.redstoneLocked);
        compound.putBoolean("Goggles", this.goggles);

        // 修复：检查流体是否为空
        if (!this.heldFluid.isEmpty()) {
            compound.put("HeldFluid", this.heldFluid.save(registries));
        } else {
            // 如果流体为空，写入一个空标记
            compound.putBoolean("EmptyFluid", true);
        }

        compound.putInt("TargetPointIndex", this.chasedPointIndex);
        compound.putFloat("MovementProgress", this.chasedPointProgress);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        int previousIndex = this.chasedPointIndex;
        Phase previousPhase = this.phase;
        ListTag interactionPointTagBefore = this.interactionPointTag;

        super.read(compound, registries, clientPacket);

        // 修复：正确处理流体读取
        if (compound.contains("EmptyFluid") && compound.getBoolean("EmptyFluid")) {
            this.heldFluid = FluidStack.EMPTY;
        } else if (compound.contains("HeldFluid")) {
            this.heldFluid = FluidStack.parseOptional(registries, compound.getCompound("HeldFluid"));
        } else {
            this.heldFluid = FluidStack.EMPTY;
        }

        this.phase = NBTHelper.readEnum(compound, "Phase", Phase.class);
        this.chasedPointIndex = compound.getInt("TargetPointIndex");
        this.chasedPointProgress = compound.getFloat("MovementProgress");
        this.interactionPointTag = compound.getList("InteractionPoints", 10);
        this.redstoneLocked = compound.getBoolean("Powered");
        boolean hadGoggles = this.goggles;
        this.goggles = compound.getBoolean("Goggles");

        if (!clientPacket)
            return;

        // 客户端特殊处理
        if (hadGoggles != this.goggles && level != null && level.isClientSide) {
            VisualizationHelper.queueUpdate(this);
        }

        boolean tagChanged = interactionPointTagBefore == null ||
                interactionPointTagBefore.size() != this.interactionPointTag.size();

        if (tagChanged) {
            this.updateInteractionPoints = true;
        }

        if (previousIndex != this.chasedPointIndex || previousPhase != this.phase) {
            FluidInteractionPoint previousPoint = null;
            if (previousPhase == Phase.MOVE_TO_INPUT && previousIndex < this.inputs.size()) {
                previousPoint = this.inputs.get(previousIndex);
            }

            if (previousPhase == Phase.MOVE_TO_OUTPUT && previousIndex < this.outputs.size()) {
                previousPoint = this.outputs.get(previousIndex);
            }

            this.previousTarget = previousPoint == null ? PipetteAngleTarget.NO_TARGET :
                    this.createAngleTarget(previousPoint);
            if (previousPoint != null) {
                this.previousBaseAngle = this.previousTarget.baseAngle;
            }

            FluidInteractionPoint targetedPoint = this.getTargetedInteractionPoint();
            if (targetedPoint != null) {
                targetedPoint.updateCachedState();
            }
        }
    }

    @Override
    public void writeSafe(CompoundTag compound, HolderLookup.Provider registries) {
        super.writeSafe(compound, registries);
        this.writeInteractionPoints(compound);

        // 添加流体保存
        if (!this.heldFluid.isEmpty()) {
            compound.put("HeldFluid", this.heldFluid.save(registries));
        } else {
            compound.putBoolean("EmptyFluid", true);
        }
    }

    @Override
    public void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
        int previousIndex = this.chasedPointIndex;
        Phase previousPhase = this.phase;
        ListTag interactionPointTagBefore = this.interactionPointTag;

        super.loadAdditional(compound, registries);

        // 修复：正确处理流体读取
        if (compound.contains("EmptyFluid") && compound.getBoolean("EmptyFluid")) {
            this.heldFluid = FluidStack.EMPTY;
        } else if (compound.contains("HeldFluid")) {
            this.heldFluid = FluidStack.parseOptional(registries, compound.getCompound("HeldFluid"));
        } else {
            this.heldFluid = FluidStack.EMPTY;
        }

        this.phase = NBTHelper.readEnum(compound, "Phase", Phase.class);
        this.chasedPointIndex = compound.getInt("TargetPointIndex");
        this.chasedPointProgress = compound.getFloat("MovementProgress");
        this.interactionPointTag = compound.getList("InteractionPoints", 10);
        this.redstoneLocked = compound.getBoolean("Powered");
        boolean hadGoggles = this.goggles;
        this.goggles = compound.getBoolean("Goggles");

        if (level != null && level.isClientSide) {
            if (hadGoggles != this.goggles) {
                // 使用 runWhenOn 替代 DistExecutor
                if (level.isClientSide) {
                    VisualizationHelper.queueUpdate(this);
                }
            }

            boolean forceUpdate = compound.getBoolean("ForceUpdate");
            boolean tagChanged = interactionPointTagBefore == null ||
                    interactionPointTagBefore.size() != this.interactionPointTag.size();

            if (forceUpdate || tagChanged) {
                this.inputs.clear();
                this.outputs.clear();

                if (this.interactionPointTag != null && this.level != null) {
                    for (Tag tag : this.interactionPointTag) {
                        FluidInteractionPoint point = FluidInteractionPoint.deserialize(
                                (CompoundTag) tag, this.level, this.worldPosition);
                        if (point != null) {
                            if (point.getMode() == FluidInteractionPoint.Mode.TAKE) {
                                this.inputs.add(point);
                            } else {
                                this.outputs.add(point);
                            }
                        }
                    }
                }

                this.updateInteractionPoints = false;
            }

            if (previousIndex != this.chasedPointIndex || previousPhase != this.phase) {
                FluidInteractionPoint previousPoint = null;
                if (previousPhase == Phase.MOVE_TO_INPUT && previousIndex < this.inputs.size()) {
                    previousPoint = this.inputs.get(previousIndex);
                }

                if (previousPhase == Phase.MOVE_TO_OUTPUT && previousIndex < this.outputs.size()) {
                    previousPoint = this.outputs.get(previousIndex);
                }

                this.previousTarget = previousPoint == null ? PipetteAngleTarget.NO_TARGET :
                        this.createAngleTarget(previousPoint);
                if (previousPoint != null) {
                    this.previousBaseAngle = this.previousTarget.baseAngle;
                }

                FluidInteractionPoint targetedPoint = this.getTargetedInteractionPoint();
                if (targetedPoint != null) {
                    targetedPoint.updateCachedState();
                }
            }
        }
    }

    public static int getRange() {
        return AllConfigs.server().logistics.mechanicalArmRange.get();
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (super.addToTooltip(tooltip, isPlayerSneaking)) {
            return true;
        } else if (isPlayerSneaking) {
            return false;
        } else if (this.tooltipWarmup > 0) {
            return false;
        } else if (!this.inputs.isEmpty()) {
            return false;
        } else if (!this.outputs.isEmpty()) {
            return false;
        } else {
            TooltipHelper.addHint(tooltip, "fluid.mechanical_pipette.no_targets");
            return true;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition, null);
        if (handler != null) {
            return this.containedFluidTooltip(tooltip, isPlayerSneaking, handler) || added;
        }

        return added;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        for (FluidInteractionPoint input : this.inputs) {
            input.setLevel(level);
        }
        for (FluidInteractionPoint output : this.outputs) {
            output.setLevel(level);
        }
    }

    // 流体能力注册
    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                com.adonis.fluid.registry.CFBlockEntities.PIPETTE.get(),
                (be, side) -> new IFluidHandler() {
                    @Override
                    public int getTanks() {
                        return 1;
                    }

                    @Override
                    public FluidStack getFluidInTank(int tank) {
                        return be.heldFluid.copy();
                    }

                    @Override
                    public int getTankCapacity(int tank) {
                        return FLUID_CAPACITY;
                    }

                    @Override
                    public boolean isFluidValid(int tank, FluidStack stack) {
                        return true;
                    }

                    @Override
                    public int fill(FluidStack resource, FluidAction action) {
                        if (resource.isEmpty()) return 0;

                        int canFill = Math.min(resource.getAmount(),
                                FLUID_CAPACITY - be.heldFluid.getAmount());
                        if (canFill <= 0) return 0;

                        if (action.execute()) {
                            if (be.heldFluid.isEmpty()) {
                                be.heldFluid = resource.copy();
                                be.heldFluid.setAmount(canFill);
                            } else if (FluidStack.isSameFluidSameComponents(be.heldFluid, resource)) {
                                be.heldFluid.grow(canFill);
                            } else {
                                return 0;
                            }
                            be.setChanged();
                            be.sendData();
                        }
                        return canFill;
                    }

                    @Override
                    public FluidStack drain(FluidStack resource, FluidAction action) {
                        if (!FluidStack.isSameFluidSameComponents(resource, be.heldFluid)) return FluidStack.EMPTY;
                        return drain(resource.getAmount(), action);
                    }

                    @Override
                    public FluidStack drain(int maxDrain, FluidAction action) {
                        if (be.heldFluid.isEmpty()) return FluidStack.EMPTY;

                        int drained = Math.min(maxDrain, be.heldFluid.getAmount());
                        FluidStack result = be.heldFluid.copy();
                        result.setAmount(drained);

                        if (action.execute()) {
                            be.heldFluid.shrink(drained);
                            be.setChanged();
                            be.sendData();
                        }
                        return result;
                    }
                }
        );
    }

    public enum Phase {
        SEARCH_INPUTS,
        MOVE_TO_INPUT,
        SEARCH_OUTPUTS,
        MOVE_TO_OUTPUT
    }

    public enum SelectionMode implements INamedIconOptions {
        ROUND_ROBIN(AllIcons.I_ARM_ROUND_ROBIN),
        FORCED_ROUND_ROBIN(AllIcons.I_ARM_FORCED_ROUND_ROBIN),
        PREFER_FIRST(AllIcons.I_ARM_PREFER_FIRST);

        private final String translationKey;
        private final AllIcons icon;

        SelectionMode(AllIcons icon) {
            this.icon = icon;
            this.translationKey = "create.mechanical_arm.selection_mode." + Lang.asId(this.name());
        }

        @Override
        public AllIcons getIcon() {
            return this.icon;
        }

        @Override
        public String getTranslationKey() {
            return this.translationKey;
        }
    }

    private class SelectionModeValueBox extends CenteredSideValueBoxTransform {
        public SelectionModeValueBox() {
            super((blockState, direction) -> !direction.getAxis().isVertical());
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            int yPos = state.getValue(PipetteBlock.CEILING) ? 13 : 3;
            Vec3 location = VecHelper.voxelSpace(8.0, yPos, 15.5);
            location = VecHelper.rotateCentered(location,
                    AngleHelper.horizontalAngle(this.getSide()), Direction.Axis.Y);
            return location;
        }

        @Override
        public float getScale() {
            return super.getScale();
        }
    }
}