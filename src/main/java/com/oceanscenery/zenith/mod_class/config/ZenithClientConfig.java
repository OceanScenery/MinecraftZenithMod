package com.oceanscenery.zenith.mod_class.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ZenithClientConfig {
    public final ModConfigSpec.BooleanValue RENDER_OFFSET;
    public final ModConfigSpec.DoubleValue TRAIL_ANGLE;

    public ZenithClientConfig(ModConfigSpec.Builder builder){
        builder.comment("渲染设置").push("render");
        RENDER_OFFSET= builder.comment("lower the sword's trail by one block").define("render_offset",false);
        TRAIL_ANGLE=builder.comment("define the trail's length(360 means full round)").defineInRange("trail_angle",120.0,80.0,200.0);
    }
}
