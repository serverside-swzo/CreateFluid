package com.adonis.fluid.config;

import net.minecraft.Util;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class CFConfig {
    private static final CFServerConfig SERVER_CONFIG = new CFServerConfig();
    private static ModConfigSpec SERVER_SPEC;

    public CFConfig(ModContainer modContainer) {
        SERVER_SPEC = Util.make(new ModConfigSpec.Builder().configure(builder -> {
            SERVER_CONFIG.registerAll(builder);
            return Unit.INSTANCE;
        }).getValue(), spec -> modContainer.registerConfig(ModConfig.Type.SERVER, spec));
    }

    public static CFServerConfig server() {
        return SERVER_CONFIG;
    }

    public static CFStressConfig stress() {
        return SERVER_CONFIG.kinetics.stressValues;
    }

    @SubscribeEvent
    public void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            SERVER_CONFIG.onLoad();
        }
    }

    @SubscribeEvent
    public void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            SERVER_CONFIG.onReload();
        }
    }
}