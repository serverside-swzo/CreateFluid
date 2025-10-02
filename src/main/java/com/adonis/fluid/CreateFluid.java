package com.adonis.fluid;

import com.adonis.fluid.config.CFStressConfig;
import com.adonis.fluid.registry.*;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

@Mod(CreateFluid.MOD_ID)
public class CreateFluid {
    public static final String MOD_ID = "fluid";
    public static final String NAME = "Create Fluid";
    public static final Random RANDOM = new Random();
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public static final CFRegistrate REGISTRATE = CFRegistrate.create(MOD_ID)
            .setTooltipModifierFactory(item ->
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
            );

    // 关键：在这里定义静态配置常量
    public static final CFStressConfig STRESS_CONFIG = new CFStressConfig(MOD_ID);

    private static ModConfigSpec stressConfigSpec;

    public CreateFluid(IEventBus modEventBus, ModContainer modContainer) {
        // 注册 Registrate
        REGISTRATE.registerEventListeners(modEventBus);

        // 注册所有内容
        CFBlocks.register();
        CFBlockEntities.register();
        CFBlockEntities.registerToEventBus(modEventBus);
        CFItems.register();
        CFCreativeTab.register(modEventBus);

        // 注册应力配置
        ModConfigSpec.Builder stressBuilder = new ModConfigSpec.Builder();
        STRESS_CONFIG.registerAll(stressBuilder);
        stressConfigSpec = stressBuilder.build();
        modContainer.registerConfig(ModConfig.Type.SERVER, stressConfigSpec, STRESS_CONFIG.getName() + ".toml");

        // 注册事件监听器
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onModConfigEvent);
        modEventBus.register(CFBlockEntities.class);

        LOGGER.info("Create Fluid initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CFPartialModels.init();
            LOGGER.info("Partial models initialized");
        });
    }

    private void onModConfigEvent(ModConfigEvent event) {
        ModConfig config = event.getConfig();

        if (stressConfigSpec != null && config.getSpec() == stressConfigSpec) {
            if (event instanceof ModConfigEvent.Loading) {
                LOGGER.info("Stress config loaded");
            } else if (event instanceof ModConfigEvent.Reloading) {
                LOGGER.info("Stress config reloaded");
            }
        }
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}