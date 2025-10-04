package com.adonis.fluid.registry;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.block.CopperTap.CopperTapBlockEntity;
import com.adonis.fluid.block.CopperTap.CopperTapRenderer;
import com.adonis.fluid.block.FluidInterface.FluidInterfaceBlockEntity;
import com.adonis.fluid.block.FluidInterface.FluidInterfaceRenderer;
import com.adonis.fluid.block.SmartFluidInterface.SmartFluidInterfaceBlockEntity;
import com.adonis.fluid.block.SmartFluidInterface.SmartFluidInterfaceRenderer;
import com.adonis.fluid.block.Pipette.PipetteBlockEntity;
import com.adonis.fluid.block.Pipette.PipetteRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.adonis.fluid.CreateFluid.REGISTRATE;

public class CFBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateFluid.MOD_ID);

    // 流体接口方块实体
    public static final BlockEntityEntry<FluidInterfaceBlockEntity> FLUID_INTERFACE = REGISTRATE
            .blockEntity("fluid_interface", FluidInterfaceBlockEntity::new)
            .validBlocks(CFBlocks.FLUID_INTERFACE)
            .renderer(() -> FluidInterfaceRenderer::new)
            .register();

    // 智能流体接口方块实体
    public static final BlockEntityEntry<SmartFluidInterfaceBlockEntity> SMART_FLUID_INTERFACE = REGISTRATE
            .blockEntity("smart_fluid_interface", SmartFluidInterfaceBlockEntity::new)
            .validBlocks(CFBlocks.SMART_FLUID_INTERFACE)
            .renderer(() -> SmartFluidInterfaceRenderer::new)
            .register();

    // 铜龙头方块实体
    public static final BlockEntityEntry<CopperTapBlockEntity> COPPER_TAP = REGISTRATE
            .blockEntity("copper_tap", CopperTapBlockEntity::new)
            .validBlocks(CFBlocks.COPPER_TAP)
            .renderer(() -> CopperTapRenderer::new)
            .register();

    // 移液器方块实体
    public static final BlockEntityEntry<PipetteBlockEntity> PIPETTE = REGISTRATE
            .blockEntity("pipette", PipetteBlockEntity::new)
            .validBlocks(CFBlocks.PIPETTE)
            .renderer(() -> PipetteRenderer::new)
            .register();

    public static void register() {
        CreateFluid.LOGGER.info("CFBlockEntities registered");
    }

    public static void registerToEventBus(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 注册移液器的流体能力
        PipetteBlockEntity.registerCapabilities(event);

        // 注册流体接口的流体能力
        FluidInterfaceBlockEntity.registerCapabilities(event);

        // 注册智能流体接口的流体能力
        SmartFluidInterfaceBlockEntity.registerCapabilities(event);

        CreateFluid.LOGGER.info("Fluid capabilities registered");
    }
}