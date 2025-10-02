package com.adonis.fluid.packet;

import com.adonis.fluid.CreateFluid;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;  // 改用这个
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// 改用 RegistryFriendlyByteBuf
public record PipetteParticlePacket(Vec3 pos, FluidStack fluid) implements CustomPacketPayload {

    public static final Type<PipetteParticlePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateFluid.MOD_ID, "pipette_particle"));

    // StreamCodec 现在使用 RegistryFriendlyByteBuf
    public static final StreamCodec<RegistryFriendlyByteBuf, PipetteParticlePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(Vec3.CODEC),
                    PipetteParticlePacket::pos,
                    FluidStack.STREAM_CODEC,
                    PipetteParticlePacket::fluid,
                    PipetteParticlePacket::new
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
        Level level = Minecraft.getInstance().level;
        if (level == null || fluid.isEmpty()) return;

        // 生成粒子效果
        for (int i = 0; i < 8; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.2;
            double offsetY = level.random.nextDouble() * 0.2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.2;

            level.addParticle(
                    ParticleTypes.FALLING_WATER,
                    pos.x + offsetX,
                    pos.y + offsetY,
                    pos.z + offsetZ,
                    0, -0.1, 0
            );
        }
    }
}