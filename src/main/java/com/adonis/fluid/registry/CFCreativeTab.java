package com.adonis.fluid.registry;

import com.adonis.fluid.CreateFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CFCreativeTab {
    private static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateFluid.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = REGISTER.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createfluid.main"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> CFBlocks.COPPER_TAP.asStack())
                    .displayItems((parameters, output) -> {
                        // 手动添加方块
                        output.accept(CFItems.BATON);
                        output.accept(CFBlocks.PIPETTE);
                        output.accept(CFBlocks.COPPER_TAP);
                        output.accept(CFBlocks.FLUID_INTERFACE);
                        output.accept(CFBlocks.SMART_FLUID_INTERFACE);

                        // 手动添加物品
                        output.accept(CFItems.HONEYCOMB_MOLD);
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}