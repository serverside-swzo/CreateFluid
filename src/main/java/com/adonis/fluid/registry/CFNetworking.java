package com.adonis.fluid.registry;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.packet.CopperTapParticlePacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = CreateFluid.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CFNetworking {
    
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        
        registrar.playToClient(
            CopperTapParticlePacket.TYPE,
            CopperTapParticlePacket.STREAM_CODEC,
            CopperTapParticlePacket::handle
        );
    }
}