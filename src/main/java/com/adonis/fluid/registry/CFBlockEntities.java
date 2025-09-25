package com.adonis.fluid.registry;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.block.CopperTap.CopperTapBlockEntity;
import com.adonis.fluid.block.CopperTap.CopperTapRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import static com.adonis.fluid.CreateFluid.REGISTRATE;

public class CFBlockEntities {
    
    // Copper Tap Block Entity
    public static final BlockEntityEntry<CopperTapBlockEntity> COPPER_TAP = REGISTRATE
            .blockEntity("copper_tap", CopperTapBlockEntity::new)
            .validBlocks(CFBlocks.COPPER_TAP)
            .renderer(() -> CopperTapRenderer::new)
            .register();
    
    public static void register() {
        // Static initialization
    }
}
