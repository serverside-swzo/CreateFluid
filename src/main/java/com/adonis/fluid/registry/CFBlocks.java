package com.adonis.fluid.registry;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.block.CopperTap.CopperTapBlock;
import com.adonis.fluid.block.FluidInterface.FluidInterfaceBlock;
import com.adonis.fluid.block.Pipette.PipetteBlock;
import com.adonis.fluid.block.SmartFluidInterface.SmartFluidInterfaceBlock;
import com.simibubi.create.foundation.data.ModelGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.Direction;
import com.adonis.fluid.item.PipetteItem;  // 添加这行导入
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

import static com.adonis.fluid.CreateFluid.REGISTRATE;

public class CFBlocks {

    // 流体接口注册
    public static final BlockEntry<FluidInterfaceBlock> FLUID_INTERFACE = REGISTRATE
            .block("fluid_interface", FluidInterfaceBlock::new)
            .initialProperties(SharedProperties::wooden)
            .properties(prop -> prop
                    .mapColor(DyeColor.BROWN)
                    .sound(SoundType.SCAFFOLDING)
                    .noOcclusion())
            .transform(TagGen.axeOrPickaxe())
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.get())
                        .forAllStates(state -> {
                            Direction dir = state.getValue(FluidInterfaceBlock.FACING);
                            int yRot = (int) dir.toYRot();
                            return ConfiguredModel.builder()
                                    .modelFile(prov.models().getExistingFile(prov.modLoc("block/fluid_interface")))
                                    .rotationY(yRot)
                                    .build();
                        });
            })
            .item()
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/fluid_interface")))
            .build()
            .register();

    // 智能流体接口注册
    public static final BlockEntry<SmartFluidInterfaceBlock> SMART_FLUID_INTERFACE = REGISTRATE
            .block("smart_fluid_interface", SmartFluidInterfaceBlock::new)
            .initialProperties(SharedProperties::wooden)
            .properties(prop -> prop
                    .mapColor(DyeColor.GRAY)
                    .sound(SoundType.METAL)
                    .noOcclusion())
            .transform(TagGen.axeOrPickaxe())
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.get())
                        .forAllStates(state -> {
                            Direction dir = state.getValue(SmartFluidInterfaceBlock.FACING);
                            int yRot = (int) dir.toYRot();
                            return ConfiguredModel.builder()
                                    .modelFile(prov.models().getExistingFile(prov.modLoc("block/smart_fluid_interface")))
                                    .rotationY(yRot)
                                    .build();
                        });
            })
            .item()
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/smart_fluid_interface")))
            .build()
            .register();

    // 在 CFBlocks.java 中添加
    public static final BlockEntry<PipetteBlock> PIPETTE = REGISTRATE
            .block("pipette", PipetteBlock::new)
            .initialProperties(SharedProperties::softMetal)
            .properties(prop -> prop
                    .mapColor(MapColor.TERRACOTTA_YELLOW)
                    .noOcclusion())
            .transform(CreateFluid.STRESS_CONFIG.setImpact(2.0))  // 完全相同的写法
            .transform(TagGen.axeOrPickaxe())
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.get())
                        .forAllStates(state -> {
                            return ConfiguredModel.builder()
                                    .modelFile(prov.models().getExistingFile(prov.modLoc("block/pipette")))
                                    .rotationX(state.getValue(PipetteBlock.CEILING) ? 180 : 0)
                                    .build();
                        });
            })
            .item(PipetteItem::new)
            .transform(ModelGen.customItemModel())
            .register();

    // 铜龙头注册
    public static final BlockEntry<CopperTapBlock> COPPER_TAP = REGISTRATE
            .block("copper_tap", CopperTapBlock::new)
            .initialProperties(SharedProperties::copperMetal)
            .properties(prop -> prop
                    .mapColor(MapColor.COLOR_ORANGE)
                    .sound(SoundType.COPPER)
                    .noOcclusion())
            .transform(TagGen.axeOrPickaxe())
            .blockstate((ctx, prov) -> {
                prov.getVariantBuilder(ctx.get())
                        .forAllStates(state -> {
                            Direction dir = state.getValue(CopperTapBlock.FACING);
                            int yRot = (int) dir.toYRot();
                            return ConfiguredModel.builder()
                                    .modelFile(prov.models().getExistingFile(prov.modLoc("block/copper_tap")))
                                    .rotationY(yRot)
                                    .build();
                        });
            })
            .item()
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/copper_tap")))
            .build()
            .register();

    public static void register() {
        // 静态初始化触发
    }
}