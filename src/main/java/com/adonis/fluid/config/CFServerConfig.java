package com.adonis.fluid.config;

import net.createmod.catnip.config.ConfigBase;

public class CFServerConfig extends ConfigBase {
    public final CFKineticsConfig kinetics;

    public CFServerConfig() {
        this.kinetics = nested(0, CFKineticsConfig::new, "Mechanical settings");
    }

    @Override
    public String getName() {
        return "server";
    }
}