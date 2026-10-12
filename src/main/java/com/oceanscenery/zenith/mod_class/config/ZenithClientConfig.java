package com.oceanscenery.zenith.mod_class.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ZenithClientConfig {
    public final ModConfigSpec.BooleanValue render_offset;
    public final ModConfigSpec.DoubleValue trail_angle;
    public final ModConfigSpec.BooleanValue _3d_trail;
    public final ModConfigSpec.BooleanValue block_interaction;

    public ZenithClientConfig(ModConfigSpec.Builder builder) {
        builder.comment("渲染设置").push("render");
        render_offset = builder.comment("lower the sword's trail by one block").define("render_offset", true);
        trail_angle = builder.comment("define the trail's length(360 means full round)").defineInRange("trail_angle", 120.0, 80.0, 200.0);
        _3d_trail = builder.comment("enable 3D trail").define("3d_trail", true);
        builder.pop();
        builder.comment("交互设置").push("interaction");
        block_interaction = builder.comment("enable block interaction").define("block_interaction", true);
    }
}
