package com.adonis.fluid.config;

import com.adonis.fluid.CreateFluid;
import net.createmod.catnip.config.ConfigBase;

public class CFKineticsConfig extends ConfigBase {
    public final CFStressConfig stressValues;

    public CFKineticsConfig() {
        this.stressValues = nested(
                0,
                () -> new CFStressConfig(CreateFluid.MOD_ID),
                "Fine tune the kinetic stats of individual components"
        );
    }

    @Override
    public String getName() {
        return "kinetics";
    }
}