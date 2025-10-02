package com.adonis.fluid;

import com.adonis.fluid.config.CFConfig;
import com.adonis.fluid.registry.*;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
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

    private static CFConfig config;

    public CreateFluid(IEventBus modEventBus, ModContainer modContainer) {
        // 初始化配置
        config = new CFConfig(modContainer);
        modEventBus.register(config);

        // 注册 Registrate
        REGISTRATE.registerEventListeners(modEventBus);

        // 注册所有内容
        CFBlocks.register();
        CFBlockEntities.register();
        CFBlockEntities.registerToEventBus(modEventBus);
        CFItems.register();
        CFCreativeTab.register(modEventBus);

        // 注册事件监听器
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.register(CFBlockEntities.class); // 注册能力事件

        LOGGER.info("Create Fluid initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CFPartialModels.init();
            LOGGER.info("Partial models initialized");
        });
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static CFConfig config() {
        return config;
    }
}