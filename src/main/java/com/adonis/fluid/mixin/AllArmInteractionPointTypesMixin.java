package com.adonis.fluid.mixin;

import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import java.lang.reflect.Method;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AllArmInteractionPointTypes.class)
public class AllArmInteractionPointTypesMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void injectMeshTrapInteractionPointType(CallbackInfo ci) {
        try {
            Method registerMethod = AllArmInteractionPointTypes.class.getDeclaredMethod("register", String.class, ArmInteractionPointType.class);
            registerMethod.setAccessible(true);

        } catch (NoSuchMethodException e) {} catch (Exception e) {}
    }
}
