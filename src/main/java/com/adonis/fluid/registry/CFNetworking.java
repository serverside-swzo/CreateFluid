package com.adonis.fluid.registry;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.packet.CopperTapParticlePacket;
import com.adonis.fluid.packet.PipetteFluidPlacementPacket;
import com.adonis.fluid.packet.PipetteParticlePacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CreateFluid.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CFNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // 铜龙头粒子包 (客户端接收)
        registrar.playToClient(
                CopperTapParticlePacket.TYPE,
                CopperTapParticlePacket.STREAM_CODEC,
                CopperTapParticlePacket::handle
        );

        // 移液器粒子包 (客户端接收)
        registrar.playToClient(
                PipetteParticlePacket.TYPE,
                PipetteParticlePacket.STREAM_CODEC,
                PipetteParticlePacket::handle
        );

        // 移液器放置配置包 (服务端接收)
        registrar.playToServer(
                PipetteFluidPlacementPacket.TYPE,
                PipetteFluidPlacementPacket.STREAM_CODEC,
                PipetteFluidPlacementPacket::handle
        );

        // 移液器客户端请求包 (客户端接收)
        registrar.playToClient(
                PipetteFluidPlacementPacket.ClientBoundRequest.TYPE,
                PipetteFluidPlacementPacket.ClientBoundRequest.STREAM_CODEC,
                PipetteFluidPlacementPacket.ClientBoundRequest::handle
        );
    }
}