package com.oceanscenery.zenith.zenith_class.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ZenithClientConfig {
    public final ForgeConfigSpec.BooleanValue RENDER_OFFSET;
    public final ForgeConfigSpec.DoubleValue TRAIL_ANGLE;
    public final ForgeConfigSpec.BooleanValue _3D_TRAIL;
    public final ForgeConfigSpec.BooleanValue BLOCK_INTERACTION;

    public ZenithClientConfig(ForgeConfigSpec.Builder builder){
        builder.comment("渲染设置").push("render");
        RENDER_OFFSET= builder.comment("lower the sword's trail by one block").define("render_offset",true);
        TRAIL_ANGLE=builder.comment("define the trail's length(360 means full round)").defineInRange("trail_angle",120.0,80.0,200.0);
        _3D_TRAIL=builder.comment("enable 3D trail").define("3d_trail",true);
        builder.pop();
        builder.comment("交互设置").push("interaction");
        BLOCK_INTERACTION=builder.comment("enable block interaction").define("block_interaction",true);
    }
}
