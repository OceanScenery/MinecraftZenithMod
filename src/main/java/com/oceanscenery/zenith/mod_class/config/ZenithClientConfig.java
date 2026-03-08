package com.oceanscenery.zenith.mod_class.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ZenithClientConfig {
    public final ModConfigSpec.BooleanValue RENDER_OFFSET;

    public ZenithClientConfig(ModConfigSpec.Builder builder){
        builder.comment("渲染设置").push("render");
        RENDER_OFFSET= builder.comment("lower the sword's trail by one block").define("render_offset",false);
    }
}
