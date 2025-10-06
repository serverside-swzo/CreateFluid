package com.adonis.fluid.packet;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.mixin.accessor.ArmBlockEntityAccessor;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ArmInteractionPointSyncPacket(BlockPos pos, ListTag pointsTag) implements CustomPacketPayload {

    public static final Type<ArmInteractionPointSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateFluid.MOD_ID, "arm_sync"));

    public static final StreamCodec<ByteBuf, ArmInteractionPointSyncPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ArmInteractionPointSyncPacket::pos,
            ByteBufCodecs.COMPOUND_TAG.map(
                    tag -> tag.getList("Points", 10),
                    points -> {
                        CompoundTag tag = new CompoundTag();
                        tag.put("Points", points);
                        return tag;
                    }
            ),
            ArmInteractionPointSyncPacket::pointsTag,
            ArmInteractionPointSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 使用实例方法
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                handleClient();
            }
        });
    }

    private void handleClient() {
        Level world = Minecraft.getInstance().level;
        if (world == null) return;

        BlockEntity be = world.getBlockEntity(this.pos);
        if (!(be instanceof ArmBlockEntity arm)) return;

        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor) arm;

        // 清空旧的交互点
        accessor.getInputs().clear();
        accessor.getOutputs().clear();

        // 设置新的标签
        accessor.setInteractionPointTag(this.pointsTag);
        accessor.setUpdateInteractionPoints(true);

        // 重置状态
        accessor.setPhase(ArmBlockEntity.Phase.SEARCH_INPUTS);
        accessor.setChasedPointProgress(0.0F);
        accessor.setChasedPointIndex(-1);

        // 立即初始化交互点
        try {
            java.lang.reflect.Method initMethod = ArmBlockEntity.class.getDeclaredMethod("initInteractionPoints");
            initMethod.setAccessible(true);
            initMethod.invoke(arm);
        } catch (Exception e) {
            // 静默处理
        }

        arm.setChanged();
    }
}