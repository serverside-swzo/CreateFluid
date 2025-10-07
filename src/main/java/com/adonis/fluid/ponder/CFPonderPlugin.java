package com.adonis.fluid.ponder;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.registry.CFBlocks;
import com.adonis.fluid.registry.CFItems;
import com.simibubi.create.Create;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class CFPonderPlugin implements PonderPlugin {

    private static final ResourceLocation FLUIDS = Create.asResource("fluids");
    private static final ResourceLocation ARM_TARGETS = Create.asResource("arm_targets");
    private static final ResourceLocation KINETIC_APPLIANCES = Create.asResource("kinetic_appliances");

    @Override
    public String getModId() {
        return CreateFluid.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderPlugin.super.registerScenes(helper);

        // 注册动力移液器的三个场景 - 使用与 1.20.1 相同的场景 ID
        helper.forComponents(CFBlocks.PIPETTE.getId())
                .addStoryBoard("pipette", PipetteScenes::setup)
                .addStoryBoard("pipette_filter", PipetteScenes::filtering)
                .addStoryBoard("pipette_fill", PipetteScenes::filling);

        // 注册指挥棒的场景
        helper.forComponents(CFItems.BATON.getId())
                .addStoryBoard("baton", ConductorBatonScenes::usage);

        // 注册铜龙头的场景
        helper.forComponents(CFBlocks.COPPER_TAP.getId())
                .addStoryBoard("tap", CopperTapScenes::tap);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderPlugin.super.registerTags(helper);

        // 将动力移液器添加到机械动力的既有标签中
        helper.addToTag(KINETIC_APPLIANCES)
                .add(CFBlocks.PIPETTE.getId());

        // 添加到流体相关标签
        helper.addToTag(FLUIDS)
                .add(CFBlocks.PIPETTE.getId())
                .add(CFBlocks.COPPER_TAP.getId());

        // 将指挥棒添加到工具标签
        helper.addToTag(ARM_TARGETS)
                .add(CFItems.BATON.getId())
                .add(CFBlocks.COPPER_TAP.getId());
    }
}