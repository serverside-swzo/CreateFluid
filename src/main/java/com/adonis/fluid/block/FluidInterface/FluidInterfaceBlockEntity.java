package com.adonis.fluid.block.FluidInterface;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.List;

public class FluidInterfaceBlockEntity extends SmartBlockEntity {

    public FluidInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // 流体接口不需要额外的行为
    }

    /**
     * 获取背后依附的容器的流体处理器
     */
    @Nullable
    public IFluidHandler getTargetFluidHandler() {
        if (level == null) return null;

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof FluidInterfaceBlock)) return null;

        Direction attachedDirection = state.getValue(FluidInterfaceBlock.FACING).getOpposite();
        BlockPos targetPos = worldPosition.relative(attachedDirection);

        // 首先尝试从流体接口面向的方向获取
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos,
                state.getValue(FluidInterfaceBlock.FACING));

        // 如果没有，尝试获取默认的
        if (handler == null) {
            handler = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, null);
        }

        return handler;
    }

    /**
     * 获取背后的目标方块实体
     */
    @Nullable
    public BlockEntity getTargetBlockEntity() {
        if (level == null) return null;

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof FluidInterfaceBlock)) return null;

        Direction attachedDirection = state.getValue(FluidInterfaceBlock.FACING).getOpposite();
        BlockPos targetPos = worldPosition.relative(attachedDirection);
        return level.getBlockEntity(targetPos);
    }

    // 不要重写 saveAdditional 和 loadAdditional，它们是 final 的
    // 如果需要保存数据，重写 write 和 read 方法

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        // 在这里添加你的自定义 NBT 保存逻辑
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        // 在这里添加你的自定义 NBT 读取逻辑
    }
}