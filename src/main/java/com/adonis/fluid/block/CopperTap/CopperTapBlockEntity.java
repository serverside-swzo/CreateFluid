package com.adonis.fluid.block.CopperTap;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.packet.CopperTapParticlePacket;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class CopperTapBlockEntity extends SmartBlockEntity {

    private FluidStack renderingFluid = FluidStack.EMPTY;
    private int processingTicks = 0;
    private static final int FILLING_TIME = 20;
    private static final int TRANSFER_RATE = 250;
    private static final int TRANSFER_INTERVAL = 10;
    private int transferCooldown = 0;
    private ItemStack processingItem = ItemStack.EMPTY;
    private boolean isFillingItem = false;
    private FluidStack pendingFluid = FluidStack.EMPTY;
    private Direction sourceDirection = null;
    private BlockPos sourceBlockPos = null;
    private int continuousProcessingDelay = 0;

    private static final TagKey<Block> TAP_FILLABLE = TagKey.create(
            Registries.BLOCK,
            CreateFluid.asResource("tap_fillable")
    );

    public CopperTapBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // Add behaviors if needed
    }

    // Internal class: Simulates waterlogged leaves as infinite water source
    private static class WaterloggedLeavesFluidHandler implements IFluidHandler {
        private static final FluidStack WATER = new FluidStack(Fluids.WATER, 1000);

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return WATER.copy();
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getFluid() == Fluids.WATER) {
                return new FluidStack(Fluids.WATER, Math.min(resource.getAmount(), 1000));
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return new FluidStack(Fluids.WATER, Math.min(maxDrain, 1000));
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null || level.isClientSide)
            return;

        BlockState state = getBlockState();
        boolean isOpen = state.getValue(BlockStateProperties.OPEN);

        if (!isOpen) {
            if (!renderingFluid.isEmpty() || !pendingFluid.isEmpty()) {
                renderingFluid = FluidStack.EMPTY;
                pendingFluid = FluidStack.EMPTY;
                isFillingItem = false;
                processingTicks = 0;
                processingItem = ItemStack.EMPTY;
                sourceDirection = null;
                sourceBlockPos = null;
                continuousProcessingDelay = 0;
                notifyUpdate();
            }
            transferCooldown = 0;
            return;
        }

        if (continuousProcessingDelay > 0) {
            continuousProcessingDelay--;
            if (continuousProcessingDelay == 0) {
                transferCooldown = 0;
            }
            return;
        }

        if (transferCooldown > 0) {
            transferCooldown--;
        }

        if (isFillingItem && processingTicks > 0) {
            processingTicks--;

            if (!validateItemStillPresent()) {
                cancelItemFilling();
                return;
            }

            if (processingTicks == 0) {
                finishItemFilling();
            }
            return;
        }

        if (transferCooldown == 0) {
            tryTransferFluid();
        }
    }

    public void onTargetChanged() {
        if (isFillingItem) {
            cancelItemFilling();
        }
        transferCooldown = 0;
        continuousProcessingDelay = 0;
        notifyUpdate();
    }

    private boolean validateItemStillPresent() {
        if (processingItem.isEmpty())
            return false;

        BlockPos targetPos = worldPosition.below();
        BlockEntity targetEntity = level.getBlockEntity(targetPos);

        if (targetEntity == null || !isDepot(targetEntity))
            return false;

        ItemStack currentItem = getItemOnDepot(targetEntity);

        return ItemStack.isSameItemSameComponents(currentItem, processingItem) &&
                currentItem.getCount() >= 1;
    }

    private void cancelItemFilling() {
        isFillingItem = false;
        processingTicks = 0;
        processingItem = ItemStack.EMPTY;
        renderingFluid = FluidStack.EMPTY;
        pendingFluid = FluidStack.EMPTY;
        sourceDirection = null;
        sourceBlockPos = null;
        continuousProcessingDelay = 0;

        level.playSound(null, worldPosition,
                net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.25f, 2.0f);

        notifyUpdate();
    }

    private void tryTransferFluid() {
        Direction attached = getBlockState().getValue(CopperTapBlock.FACING);
        BlockPos sourcePos = worldPosition.relative(attached.getOpposite());
        BlockState sourceState = level.getBlockState(sourcePos);

        IFluidHandler sourceHandler = null;

        if (sourceState.is(BlockTags.LEAVES)) {
            if (sourceState.hasProperty(BlockStateProperties.WATERLOGGED) &&
                    sourceState.getValue(BlockStateProperties.WATERLOGGED)) {
                sourceHandler = new WaterloggedLeavesFluidHandler();
            } else {
                if (!renderingFluid.isEmpty()) {
                    renderingFluid = FluidStack.EMPTY;
                    notifyUpdate();
                }
                return;
            }
        } else {
            BlockEntity sourceEntity = level.getBlockEntity(sourcePos);
            if (sourceEntity == null) {
                if (!renderingFluid.isEmpty()) {
                    renderingFluid = FluidStack.EMPTY;
                    notifyUpdate();
                }
                return;
            }

            sourceHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, sourcePos, attached);
            if (sourceHandler == null) {
                sourceHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, sourcePos, null);
            }
        }

        if (sourceHandler == null) {
            if (!renderingFluid.isEmpty()) {
                renderingFluid = FluidStack.EMPTY;
                notifyUpdate();
            }
            return;
        }

        BlockPos targetPos = worldPosition.below();
        boolean success = tryProcess(sourceHandler, targetPos, attached, sourcePos);

        if (success) {
            transferCooldown = 5;
        } else {
            if (!renderingFluid.isEmpty()) {
                renderingFluid = FluidStack.EMPTY;
                notifyUpdate();
            }
        }
    }

    private boolean tryProcess(IFluidHandler sourceHandler, BlockPos targetPos, Direction sourceDir, BlockPos sourcePos) {
        BlockEntity targetEntity = level.getBlockEntity(targetPos);
        BlockState targetState = level.getBlockState(targetPos);

        if (targetEntity != null && isDepot(targetEntity)) {
            ItemStack itemOnDepot = getItemOnDepot(targetEntity);
            if (!itemOnDepot.isEmpty() && FillingBySpout.canItemBeFilled(level, itemOnDepot)) {
                return startItemFilling(sourceHandler, targetPos, itemOnDepot, sourceDir, sourcePos);
            }
            return false;
        }

        if (targetState.is(Blocks.CAULDRON) || targetState.is(Blocks.WATER_CAULDRON)) {
            return tryFillCauldron(sourceHandler, targetPos, targetState);
        }

        if (targetState.is(TAP_FILLABLE) && targetEntity != null) {
            return tryFillContainer(sourceHandler, targetEntity);
        }

        return false;
    }

    private boolean tryFillCauldron(IFluidHandler sourceHandler, BlockPos targetPos, BlockState targetState) {
        FluidStack availableFluid = sourceHandler.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        if (availableFluid.isEmpty()) {
            return false;
        }

        if (availableFluid.getFluid() == Fluids.WATER) {
            if (targetState.is(Blocks.CAULDRON)) {
                return fillWaterCauldronLevel(sourceHandler, targetPos, targetState, 1);
            } else if (targetState.is(Blocks.WATER_CAULDRON)) {
                if (targetState.hasProperty(LayeredCauldronBlock.LEVEL)) {
                    int currentLevel = targetState.getValue(LayeredCauldronBlock.LEVEL);
                    if (currentLevel < LayeredCauldronBlock.MAX_FILL_LEVEL) {
                        return fillWaterCauldronLevel(sourceHandler, targetPos, targetState, currentLevel + 1);
                    }
                }
            }
            return false;
        }

        if (!targetState.is(Blocks.CAULDRON)) {
            return false;
        }

        var cauldronInfo = com.simibubi.create.api.behaviour.spouting.CauldronSpoutingBehavior
                .CAULDRON_INFO.get(availableFluid.getFluid());

        if (cauldronInfo == null) {
            return false;
        }

        if (availableFluid.getAmount() < cauldronInfo.amount()) {
            return false;
        }

        FluidStack drained = sourceHandler.drain(cauldronInfo.amount(), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty() || drained.getAmount() < cauldronInfo.amount()) {
            return false;
        }

        level.setBlockAndUpdate(targetPos, cauldronInfo.cauldron());
        renderingFluid = drained.copy();

        level.playSound(null, targetPos,
                net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.5f, 1.0f);

        sendFillingParticles(targetPos, drained);
        notifyUpdate();
        return true;
    }

    private boolean fillWaterCauldronLevel(IFluidHandler sourceHandler, BlockPos targetPos,
                                           BlockState currentState, int targetLevel) {
        int requiredAmount = 250;

        FluidStack availableWater = sourceHandler.drain(requiredAmount, IFluidHandler.FluidAction.SIMULATE);
        if (availableWater.isEmpty() || availableWater.getAmount() < requiredAmount ||
                availableWater.getFluid() != Fluids.WATER) {
            return false;
        }

        FluidStack drained = sourceHandler.drain(requiredAmount, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty() || drained.getAmount() < requiredAmount) {
            return false;
        }

        BlockState newState = Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, targetLevel);
        level.setBlockAndUpdate(targetPos, newState);

        renderingFluid = drained.copy();

        float pitch = 0.8f + (targetLevel * 0.1f);
        level.playSound(null, targetPos,
                net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.5f, pitch);

        sendFillingParticles(targetPos, drained);
        notifyUpdate();
        return true;
    }

    private boolean startItemFilling(IFluidHandler sourceHandler, BlockPos targetPos, ItemStack item,
                                     Direction sourceDir, BlockPos sourcePos) {
        FluidStack availableFluid = sourceHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (availableFluid.isEmpty())
            return false;

        int requiredAmount = FillingBySpout.getRequiredAmountForItem(level, item, availableFluid);
        if (requiredAmount <= 0 || requiredAmount > availableFluid.getAmount())
            return false;

        FluidStack simulatedDrain = sourceHandler.drain(requiredAmount, IFluidHandler.FluidAction.SIMULATE);
        if (simulatedDrain.isEmpty() || simulatedDrain.getAmount() < requiredAmount)
            return false;

        isFillingItem = true;
        processingTicks = FILLING_TIME;
        processingItem = item.copy();
        processingItem.setCount(1);
        pendingFluid = simulatedDrain.copy();
        renderingFluid = simulatedDrain.copy();
        sourceDirection = sourceDir;
        sourceBlockPos = sourcePos.immutable();

        AllSoundEvents.SPOUTING.playOnServer(level, worldPosition, 0.75f, 0.9f + 0.2f * level.random.nextFloat());

        sendFillingParticles(targetPos, simulatedDrain);
        notifyUpdate();
        return true;
    }

    private void sendFillingParticles(BlockPos targetPos, FluidStack fluid) {
        if (level.isClientSide || fluid.isEmpty())
            return;

        Vec3 startPos = Vec3.atCenterOf(worldPosition).add(0, -0.25, 0);
        Vec3 endPos = Vec3.atCenterOf(targetPos).add(0, 0.5, 0);

        PacketDistributor.sendToPlayersTrackingChunk(
                (ServerLevel) level,
                level.getChunkAt(targetPos).getPos(),
                new CopperTapParticlePacket(startPos, endPos, fluid)
        );
    }

    private void finishItemFilling() {
        if (!isFillingItem || processingItem.isEmpty() || pendingFluid.isEmpty())
            return;

        BlockPos targetPos = worldPosition.below();
        BlockEntity targetEntity = level.getBlockEntity(targetPos);

        if (targetEntity != null && isDepot(targetEntity)) {
            ItemStack currentItem = getItemOnDepot(targetEntity);
            if (!ItemStack.isSameItemSameComponents(currentItem, processingItem) ||
                    currentItem.getCount() < 1) {
                cancelItemFilling();
                return;
            }

            boolean fluidConsumed = false;
            if (sourceBlockPos != null && sourceDirection != null) {
                BlockState sourceState = level.getBlockState(sourceBlockPos);

                if (sourceState.is(BlockTags.LEAVES) &&
                        sourceState.hasProperty(BlockStateProperties.WATERLOGGED) &&
                        sourceState.getValue(BlockStateProperties.WATERLOGGED) &&
                        pendingFluid.getFluid() == Fluids.WATER) {
                    fluidConsumed = true;
                } else {
                    BlockEntity sourceEntity = level.getBlockEntity(sourceBlockPos);
                    if (sourceEntity != null) {
                        IFluidHandler sourceHandler = level.getCapability(
                                Capabilities.FluidHandler.BLOCK, sourceBlockPos, sourceDirection);
                        if (sourceHandler == null) {
                            sourceHandler = level.getCapability(
                                    Capabilities.FluidHandler.BLOCK, sourceBlockPos, null);
                        }

                        if (sourceHandler != null) {
                            FluidStack drained = sourceHandler.drain(pendingFluid, IFluidHandler.FluidAction.EXECUTE);
                            if (!drained.isEmpty() && drained.getAmount() >= pendingFluid.getAmount()) {
                                fluidConsumed = true;
                            }
                        }
                    }
                }
            }

            if (!fluidConsumed) {
                cancelItemFilling();
                return;
            }

            var behaviour = com.simibubi.create.content.logistics.depot.DepotBehaviour.get(
                    targetEntity, com.simibubi.create.content.logistics.depot.DepotBehaviour.TYPE);

            if (behaviour != null) {
                ItemStack itemOnDepot = behaviour.getHeldItemStack();

                if (!itemOnDepot.isEmpty()) {
                    ItemStack singleItem = itemOnDepot.copy();
                    singleItem.setCount(1);

                    ItemStack result = FillingBySpout.fillItem(level, pendingFluid.getAmount(),
                            singleItem, pendingFluid);

                    if (!result.isEmpty()) {
                        itemOnDepot.shrink(1);

                        if (itemOnDepot.isEmpty()) {
                            var resultTIS = new com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack(result);
                            behaviour.setHeldItem(resultTIS);
                        } else {
                            var updatedTIS = new com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack(itemOnDepot);
                            behaviour.setHeldItem(updatedTIS);

                            try {
                                java.lang.reflect.Field bufferField = com.simibubi.create.content.logistics.depot.DepotBehaviour.class.getDeclaredField("processingOutputBuffer");
                                bufferField.setAccessible(true);
                                net.neoforged.neoforge.items.ItemStackHandler outputBuffer =
                                        (net.neoforged.neoforge.items.ItemStackHandler) bufferField.get(behaviour);

                                ItemStack remainder = result.copy();
                                for (int slot = 0; slot < outputBuffer.getSlots() && !remainder.isEmpty(); slot++) {
                                    remainder = outputBuffer.insertItem(slot, remainder, false);
                                }

                                if (!remainder.isEmpty()) {
                                    Vec3 dropPos = Vec3.atCenterOf(targetPos);
                                    net.minecraft.world.Containers.dropItemStack(
                                            level,
                                            dropPos.x,
                                            dropPos.y + 0.5,
                                            dropPos.z,
                                            remainder
                                    );
                                }
                            } catch (Exception e) {
                                Vec3 dropPos = Vec3.atCenterOf(targetPos);
                                net.minecraft.world.Containers.dropItemStack(
                                        level,
                                        dropPos.x,
                                        dropPos.y + 0.5,
                                        dropPos.z,
                                        result
                                );
                            }
                        }

                        targetEntity.setChanged();

                        if (!level.isClientSide) {
                            level.sendBlockUpdated(targetPos, targetEntity.getBlockState(),
                                    targetEntity.getBlockState(), 3);

                            if (targetEntity instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity smartBE) {
                                smartBE.notifyUpdate();
                            }
                        }

                        level.playSound(null, targetPos,
                                net.minecraft.sounds.SoundEvents.BOTTLE_FILL,
                                net.minecraft.sounds.SoundSource.BLOCKS,
                                0.5f, 1.0f + level.random.nextFloat() * 0.2f);

                        sendFillingParticles(targetPos, renderingFluid);

                        if (itemOnDepot.isEmpty() && FillingBySpout.canItemBeFilled(level, result)) {
                            continuousProcessingDelay = 0;

                            isFillingItem = false;
                            processingTicks = 0;
                            processingItem = ItemStack.EMPTY;
                            renderingFluid = FluidStack.EMPTY;
                            pendingFluid = FluidStack.EMPTY;

                            notifyUpdate();
                            return;
                        }
                    }
                }
            }
        }

        isFillingItem = false;
        processingTicks = 0;
        processingItem = ItemStack.EMPTY;
        renderingFluid = FluidStack.EMPTY;
        pendingFluid = FluidStack.EMPTY;
        sourceDirection = null;
        sourceBlockPos = null;
        continuousProcessingDelay = 0;
        notifyUpdate();
    }

    private boolean tryFillContainer(IFluidHandler sourceHandler, BlockEntity targetEntity) {
        IFluidHandler targetHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, 
                targetEntity.getBlockPos(), Direction.UP);
        if (targetHandler == null) {
            targetHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, 
                    targetEntity.getBlockPos(), null);
        }

        if (targetHandler == null)
            return false;

        FluidStack drain = sourceHandler.drain(TRANSFER_RATE, IFluidHandler.FluidAction.SIMULATE);
        if (drain.isEmpty())
            return false;

        int filled = targetHandler.fill(drain, IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0)
            return false;

        FluidStack actualDrain = sourceHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        if (actualDrain.isEmpty())
            return false;

        targetHandler.fill(actualDrain, IFluidHandler.FluidAction.EXECUTE);

        renderingFluid = actualDrain.copy();

        if (level.random.nextFloat() < 0.1f) {
            AllSoundEvents.SPOUTING.playOnServer(level, worldPosition, 0.3f, 0.9f + 0.2f * level.random.nextFloat());
        }

        sendFillingParticles(targetEntity.getBlockPos(), actualDrain);
        notifyUpdate();
        return true;
    }

    private boolean isDepot(BlockEntity entity) {
        return entity.getClass().getSimpleName().toLowerCase().contains("depot");
    }

    private ItemStack getItemOnDepot(BlockEntity depot) {
        if (depot instanceof com.simibubi.create.content.logistics.depot.DepotBlockEntity depotEntity) {
            return depotEntity.getHeldItem();
        }

        var behaviour = com.simibubi.create.content.logistics.depot.DepotBehaviour.get(
                depot, com.simibubi.create.content.logistics.depot.DepotBehaviour.TYPE);
        if (behaviour != null) {
            return behaviour.getHeldItemStack();
        }

        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, depot.getBlockPos(), Direction.UP);
        if (handler != null && handler.getSlots() > 0) {
            return handler.getStackInSlot(0);
        }

        return ItemStack.EMPTY;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);

        // 只有非空的 FluidStack 才保存
        if (!renderingFluid.isEmpty()) {
            tag.put("RenderingFluid", renderingFluid.save(registries));
        }
        if (!pendingFluid.isEmpty()) {
            tag.put("PendingFluid", pendingFluid.save(registries));
        }

        tag.putBoolean("IsFillingItem", isFillingItem);
        tag.putInt("ProcessingTicks", processingTicks);
        tag.putInt("TransferCooldown", transferCooldown);
        tag.putInt("ContinuousProcessingDelay", continuousProcessingDelay);

        if (!processingItem.isEmpty()) {
            tag.put("ProcessingItem", processingItem.save(registries));
        }
        if (sourceDirection != null) {
            tag.putInt("SourceDirection", sourceDirection.get3DDataValue());
        }
        if (sourceBlockPos != null) {
            tag.putLong("SourcePos", sourceBlockPos.asLong());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        // 读取时检查标签是否存在
        if (tag.contains("RenderingFluid")) {
            renderingFluid = FluidStack.parse(registries, tag.getCompound("RenderingFluid")).orElse(FluidStack.EMPTY);
        } else {
            renderingFluid = FluidStack.EMPTY;
        }

        if (tag.contains("PendingFluid")) {
            pendingFluid = FluidStack.parse(registries, tag.getCompound("PendingFluid")).orElse(FluidStack.EMPTY);
        } else {
            pendingFluid = FluidStack.EMPTY;
        }

        isFillingItem = tag.getBoolean("IsFillingItem");
        processingTicks = tag.getInt("ProcessingTicks");
        transferCooldown = tag.getInt("TransferCooldown");
        continuousProcessingDelay = tag.getInt("ContinuousProcessingDelay");

        if (tag.contains("ProcessingItem")) {
            processingItem = ItemStack.parse(registries, tag.getCompound("ProcessingItem")).orElse(ItemStack.EMPTY);
        } else {
            processingItem = ItemStack.EMPTY;
        }

        if (tag.contains("SourceDirection")) {
            sourceDirection = Direction.from3DDataValue(tag.getInt("SourceDirection"));
        } else {
            sourceDirection = null;
        }

        if (tag.contains("SourcePos")) {
            sourceBlockPos = BlockPos.of(tag.getLong("SourcePos"));
        } else {
            sourceBlockPos = null;
        }
    }

    public FluidStack getRenderingFluid() {
        return renderingFluid;
    }

    public int getProcessingTicks() {
        return processingTicks;
    }

    public boolean isProcessing() {
        return isFillingItem && processingTicks > 0;
    }

    public boolean hasFluidToRender() {
        return !renderingFluid.isEmpty();
    }
}