package com.adonis.fluid.registry;

import com.adonis.fluid.CreateFluid;
import com.adonis.fluid.block.CopperTap.CopperTapRenderer;
import com.adonis.fluid.ponder.CFPonderPlugin;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CreateFluid.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CFClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        CFPartialModels.init(); // 如果你有 PartialModels

        event.enqueueWork(() -> {
            // 设置渲染层
            ItemBlockRenderTypes.setRenderLayer(CFBlocks.COPPER_TAP.get(), RenderType.cutout());

            // 注册方块实体渲染器
            BlockEntityRenderers.register(CFBlockEntities.COPPER_TAP.get(), CopperTapRenderer::new);

            // 注册 Ponder 插件 - 这是关键！
            PonderIndex.addPlugin(new CFPonderPlugin());
        });
    }
}