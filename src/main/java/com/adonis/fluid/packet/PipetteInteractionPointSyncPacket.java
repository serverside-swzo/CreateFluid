package com.adonis.fluid.packet;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.block.Pipette.PipetteBlockEntity;
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

public record PipetteInteractionPointSyncPacket(BlockPos pos, ListTag pointsTag) implements CustomPacketPayload {

    public static final Type<PipetteInteractionPointSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateFluid.MOD_ID, "pipette_sync"));

    public static final StreamCodec<ByteBuf, PipetteInteractionPointSyncPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            PipetteInteractionPointSyncPacket::pos,
            ByteBufCodecs.COMPOUND_TAG.map(
                    tag -> tag.getList("Points", 10),
                    points -> {
                        CompoundTag tag = new CompoundTag();
                        tag.put("Points", points);
                        return tag;
                    }
            ),
            PipetteInteractionPointSyncPacket::pointsTag,
            PipetteInteractionPointSyncPacket::new
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
        if (!(be instanceof PipetteBlockEntity pipette)) return;

        pipette.inputs.clear();
        pipette.outputs.clear();

        pipette.setInteractionPointTag(this.pointsTag);
        pipette.setUpdateInteractionPoints(true);

        // 立即初始化
        pipette.forceInitInteractionPoints();

        pipette.resetMovementState();
        pipette.setChanged();
    }
}