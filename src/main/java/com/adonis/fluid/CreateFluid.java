package com.adonis.fluid;

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

import java.util.Random;

@Mod(CreateFluid.MOD_ID)
public class CreateFluid {
    public static final String MOD_ID = "fluid";
    public static final String NAME = "Create Fluid";
    public static final Random RANDOM = new Random();

    // 使用自定义的 Registrate，完全仿照 CreateFisheryIndustry
    public static final CFRegistrate REGISTRATE = CFRegistrate.create(MOD_ID)
            .setTooltipModifierFactory(item ->
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
            );

    public CreateFluid(IEventBus modEventBus, ModContainer modContainer) {
        // Register Registrate
        REGISTRATE.registerEventListeners(modEventBus);

        // Register content - 按照 CreateFisheryIndustry 的顺序
        CFBlocks.register();
        CFBlockEntities.register();
        CFItems.register();
        CFCreativeTab.register(modEventBus);
        CFPartialModels.register();

        // Register events
        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialize partial models
            CFPartialModels.init();
        });
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}