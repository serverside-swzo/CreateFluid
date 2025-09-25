package com.adonis.fluid.registry;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.block.CopperTap.CopperTapBlock;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

import static com.adonis.fluid.CreateFluid.REGISTRATE;

public class CFBlocks {

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
            // 不要添加 .tab() 方法，让手动添加来控制
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/copper_tap")))
            .build()
            .register();

    public static void register() {
        // Static initialization
    }
}