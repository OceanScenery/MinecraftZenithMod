package com.oceanscenery.zenith.mod_class.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ZenithConfig {
    public final ModConfigSpec.DoubleValue ensured_damage_for_non_player;
    public final ModConfigSpec.DoubleValue ensured_damage_for_player;
    public final ModConfigSpec.DoubleValue default_damage;
    public final ModConfigSpec.DoubleValue ranged_damage_factor;
    public final ModConfigSpec.BooleanValue sort_farest;
    public final ModConfigSpec.BooleanValue disable_knockback;
    public final ModConfigSpec.BooleanValue enable_bypass_invulnerable;
    public final ModConfigSpec.BooleanValue enable_attack_item;
    public final ModConfigSpec.BooleanValue enable_terra_damage_modifier;

    public ZenithConfig(ModConfigSpec.Builder builder){
        builder.comment("combat").push("attack_damage");
        ensured_damage_for_non_player=builder.comment("minimum damage ratio for non-player (0.0-1.0), damage will be raised to this ratio of original damage if lower").defineInRange("ensured_damage_for_non_player",0.2f,0f,1f);
        ensured_damage_for_player=builder.comment("minimum damage ratio for player (0.0-1.0), damage will be raised to this ratio of original damage if lower").defineInRange("ensured_damage_for_player",0.1f,0f,1f);
        default_damage =builder.comment("the default attribute-attack_damage of the item").defineInRange("default_damage",8,0,Float.MAX_VALUE);
        ranged_damage_factor=builder.comment("final ranged damage would be initial damage*this factor").defineInRange("ranged_damage_factor",0.1,0.0,1.0);
        sort_farest=builder.comment("whether to sort farest enemy or nearest").define("sort_farest",true);
        disable_knockback= builder.comment("whether to disable knockback").define("disable_knockback",true);
        enable_bypass_invulnerable=builder.comment("whether to bypass the damage reduction of living entity(which may disrupt game balance,not recommended)").define("enable_bypass_invulnerable",false);
        enable_attack_item=builder.comment("whether to attack item or xp orb in \"all\" mode").define("enable_attack_item",false);
        enable_terra_damage_modifier=builder.comment("whether to enable the attack boost when mod \"confluence\" is loaded").define("enable_terra_damage_modifier",true);
    }
}
