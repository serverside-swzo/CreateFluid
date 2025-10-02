package com.adonis.fluid.packet;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.block.Pipette.PipetteBlockEntity;
import com.adonis.fluid.content.pipette.FluidInteractionPoint;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Collection;

public record PipetteFluidPlacementPacket(ListTag pointsTag, BlockPos pos) implements CustomPacketPayload {

    public static final Type<PipetteFluidPlacementPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateFluid.MOD_ID, "pipette_placement"));

    public static final StreamCodec<ByteBuf, PipetteFluidPlacementPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG.map(
                    tag -> tag.getList("Points", 10),
                    points -> {
                        CompoundTag tag = new CompoundTag();
                        tag.put("Points", points);
                        return tag;
                    }
            ),
            PipetteFluidPlacementPacket::pointsTag,
            BlockPos.STREAM_CODEC,
            PipetteFluidPlacementPacket::pos,
            PipetteFluidPlacementPacket::new
    );

    public PipetteFluidPlacementPacket(Collection<FluidInteractionPoint> points, BlockPos pos) {
        this(createPointsTag(points, pos), pos);
    }

    private static ListTag createPointsTag(Collection<FluidInteractionPoint> points, BlockPos pos) {
        ListTag tag = new ListTag();
        points.stream()
                .map(point -> point.serialize(pos))
                .forEach(tag::add);
        return tag;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            Level world = player.level();
            if (!world.isLoaded(pos)) return;

            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof PipetteBlockEntity pipette) {
                pipette.inputs.clear();
                pipette.outputs.clear();

                pipette.setInteractionPointTag(pointsTag);
                pipette.resetMovementState();
                pipette.forceReloadInteractionPoints();

                pipette.setChanged();
                pipette.sendData();

                world.getServer().execute(() -> {
                    world.sendBlockUpdated(pos, pipette.getBlockState(), pipette.getBlockState(), 3);
                });
            }
        });
    }

    // 客户端请求包
    public record ClientBoundRequest(BlockPos pos) implements CustomPacketPayload {

        public static final Type<ClientBoundRequest> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(CreateFluid.MOD_ID, "pipette_request"));

        public static final StreamCodec<ByteBuf, ClientBoundRequest> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                ClientBoundRequest::pos,
                ClientBoundRequest::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public void handle(IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.flow().isClientbound()) {
                    handleClient();
                }
            });
        }

        private void handleClient() {
            com.adonis.fluid.handler.PipetteFluidInteractionPointHandler.flushSettings(pos);
        }
    }
}