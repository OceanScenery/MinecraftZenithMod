package com.oceanscenery.zenith.mod_class.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ZenithConfig {
    public final ModConfigSpec.DoubleValue ensured_damage_for_non_player;
    public final ModConfigSpec.DoubleValue ensured_damage_for_player;
    public final ModConfigSpec.DoubleValue default_damage;
    public final ModConfigSpec.DoubleValue ranged_damage_factor;
    public final ModConfigSpec.BooleanValue sort_farest;
    public final ModConfigSpec.BooleanValue disable_knockback;

    public ZenithConfig(ModConfigSpec.Builder builder){
        builder.comment("combat").push("attack_damage");
        ensured_damage_for_non_player=builder.comment("damage that lower than this(for non-player) will be modified to this").defineInRange("ensured_damage_for_non_player",1,0,Float.MAX_VALUE);
        ensured_damage_for_player=builder.comment("damage that lower than this(for player) will be modified to this").defineInRange("ensured_damage_for_player",0.1,0,Float.MAX_VALUE);
        default_damage =builder.comment("the default attribute-attack_damage of the item").defineInRange("default_damage",8,0,Float.MAX_VALUE);
        ranged_damage_factor=builder.comment("final ranged damage would be initial damage*this factor").defineInRange("ranged_damage_factor",0.2,0.0,1.0);
        sort_farest=builder.comment("whether to sort farest enemy or nearest").define("sort_farest",true);
        disable_knockback= builder.comment("whether to disable knockback").define("disable_knockback",true);
    }
}
