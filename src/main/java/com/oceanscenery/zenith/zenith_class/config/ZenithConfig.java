package com.oceanscenery.zenith.zenith_class.config;


import net.minecraftforge.common.ForgeConfigSpec;

public class ZenithConfig {
    public static final String FORCE_REMOVAL_DESCRIPTION_CH = "若为true，则天顶剑攻击在敌方血量降至0时(或者伤害无效时)会以较为激进的手段移除敌人并生成掉落物,此过程可能不稳定或者产生多倍掉落物,如果你遇到了问题,请关闭此选项";
    public static final String FORCE_REMOVAL_DESCRIPTION_EN = "If true, the Zenith Sword aggressively removes enemies and generates loot when their health reaches 0 or an attack deals no effective damage. This may be unstable or cause loot to drop multiple times. Turn this option off if you encounter problems.";
    public final ForgeConfigSpec.DoubleValue ensured_damage_for_non_player;
    public final ForgeConfigSpec.DoubleValue ensured_damage_for_player;
    public final ForgeConfigSpec.DoubleValue default_damage;
    public final ForgeConfigSpec.DoubleValue ranged_damage_factor;
    public final ForgeConfigSpec.BooleanValue sort_farest;
    public final ForgeConfigSpec.BooleanValue disable_knockback;
    public final ForgeConfigSpec.BooleanValue enable_bypass_invulnerable;
    public final ForgeConfigSpec.BooleanValue enable_attack_item;
    public final ForgeConfigSpec.BooleanValue enable_terra_damage_modifier;
    public final ForgeConfigSpec.DoubleValue percentage_damage_factor;
    public final ForgeConfigSpec.BooleanValue forceRemoval;

    public ZenithConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("combat").push("attack_damage");
        ensured_damage_for_non_player = builder.comment("minimum damage ratio for non-player (0.0-1.0), damage will be raised to this ratio of original damage if lower").defineInRange("ensured_damage_for_non_player", 0.2f, 0f, 1f);
        ensured_damage_for_player = builder.comment("minimum damage ratio for player (0.0-1.0), damage will be raised to this ratio of original damage if lower").defineInRange("ensured_damage_for_player", 0.1f, 0f, 1f);
        default_damage = builder.comment("the default attribute-attack_damage of the item").defineInRange("default_damage", 8, 0, Float.MAX_VALUE);
        ranged_damage_factor = builder.comment("final ranged damage would be initial damage*this factor").defineInRange("ranged_damage_factor", 0.1, 0.0, 1.0);
        sort_farest = builder.comment("whether to sort farest enemy or nearest").define("sort_farest", true);
        disable_knockback = builder.comment("whether to disable knockback").define("disable_knockback", true);
        enable_bypass_invulnerable = builder.comment("whether to bypass the damage reduction of living entity(which may disrupt game balance,not recommended)").define("enable_bypass_invulnerable", false);
        enable_attack_item = builder.comment("whether to attack item or xp orb in \"all\" mode").define("enable_attack_item", false);
        enable_terra_damage_modifier = builder.comment("whether to enable the attack boost when mod \"confluence\" is loaded").define("enable_terra_damage_modifier", true);
        percentage_damage_factor = builder.comment("determines the percentage of the enemy’s max health dealt as bonus damage on attack").defineInRange("percentage_damage_factor", 0.01f, 0f, 1f);
        forceRemoval = builder.comment(FORCE_REMOVAL_DESCRIPTION_EN).define("invalid_damage_action", false);
        builder.pop();
    }
}
