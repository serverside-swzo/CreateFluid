package com.adonis.fluid.packet;

import com.adonis.fluid.CreateFluid;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PipetteParticlePacket(Vec3 pos, FluidStack fluid) implements CustomPacketPayload {

    public static final Type<PipetteParticlePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreateFluid.MOD_ID, "pipette_particle"));

    // 使用手动编码/解码而不是 composite
    public static final StreamCodec<RegistryFriendlyByteBuf, PipetteParticlePacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PipetteParticlePacket decode(RegistryFriendlyByteBuf buffer) {
            double x = buffer.readDouble();
            double y = buffer.readDouble();
            double z = buffer.readDouble();
            Vec3 pos = new Vec3(x, y, z);

            // 读取流体
            FluidStack fluid = FluidStack.OPTIONAL_STREAM_CODEC.decode(buffer);

            return new PipetteParticlePacket(pos, fluid);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PipetteParticlePacket packet) {
            buffer.writeDouble(packet.pos.x);
            buffer.writeDouble(packet.pos.y);
            buffer.writeDouble(packet.pos.z);

            // 写入流体
            FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.fluid);
        }
    };

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

        // 使用 Create 的流体粒子效果
        ParticleOptions particle = com.simibubi.create.content.fluids.FluidFX.getFluidParticle(fluid);

        // 生成向下喷洒的粒子效果
        for (int i = 0; i < 20; i++) {
            Vec3 motion = net.createmod.catnip.math.VecHelper.offsetRandomly(
                    Vec3.ZERO, level.random, 0.125F);
            motion = new Vec3(motion.x, -Math.abs(motion.y) * 0.5 - 0.1, motion.z);

            // 使用 addParticle 而不是 addAlwaysVisibleParticle
            level.addParticle(particle,
                    pos.x, pos.y, pos.z,
                    motion.x, motion.y, motion.z);
        }
    }
}