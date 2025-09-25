package com.adonis.fluid.ponder;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.registry.CFBlocks;
import com.simibubi.create.Create;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class CFPonderPlugin implements PonderPlugin {

    private static final ResourceLocation FLUIDS = Create.asResource("fluids");
    private static final ResourceLocation ARM_TARGETS = Create.asResource("arm_targets");

    @Override
    public String getModId() {
        return CreateFluid.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderPlugin.super.registerScenes(helper);

        // 使用 BlockEntry 的 getId() 方法
        helper.forComponents(CFBlocks.COPPER_TAP.getId())
                .addStoryBoard("tap", CopperTapScenes::tap);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderPlugin.super.registerTags(helper);

        // 添加到 Create 的标签
        helper.addToTag(FLUIDS)
                .add(CFBlocks.COPPER_TAP.getId());

        helper.addToTag(ARM_TARGETS)
                .add(CFBlocks.COPPER_TAP.getId());
    }
}