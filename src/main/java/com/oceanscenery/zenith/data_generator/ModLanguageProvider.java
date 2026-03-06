package com.oceanscenery.zenith.data_generator;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ModConfigs;
import com.oceanscenery.zenith.registry.ModEntity;
import com.oceanscenery.zenith.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    private final String locale;
    public ModLanguageProvider(PackOutput output,String locale) {
        super(output, TheZenithMod.MOD_ID, locale);
        this.locale=locale;
    }

    @Override
    protected void addTranslations() {
        if(locale.equals("zh_cn")){
            add(ModItems.ZENITH.get(),"天顶剑");
            add(ModEntity.ZENITH_PROJECTILE.get(),"天顶剑射弹");
            add("the_zenith_sword.configuration.attack_damage","伤害设置");
            add("the_zenith_sword.configuration.ensured_damage_for_player","对玩家保底伤害");
            add("the_zenith_sword.configuration.ensured_damage_for_non_player","对非玩家保底伤害");
            add("the_zenith_sword.configuration.default_damage","物品默认攻击");
            add("the_zenith_sword.configuration.ranged_damage_factor","远程伤害系数");
            add("the_zenith_sword.configuration.sort_farest","优先搜索远方敌人");
            add("the_zenith_sword.configuration.disable_knockback","取消击退");
        }
        if(locale.equals("en_us")){
            add(ModItems.ZENITH.get(),"Zenith Sword");
            add(ModEntity.ZENITH_PROJECTILE.get(),"ZenithProjectile");
            add("the_zenith_sword.configuration.attack_damage","damage settings");
            add("the_zenith_sword.configuration.ensured_damage_for_player","ensured damage when attacking player");
            add("the_zenith_sword.configuration.ensured_damage_for_non_player","ensured damage when attacking non-player entity");
            add("the_zenith_sword.configuration.default_damage","default damage");
            add("the_zenith_sword.configuration.ranged_damage_factor","ranged damage factor");
            add("the_zenith_sword.configuration.sort_farest","sort farest enemy first");
            add("the_zenith_sword.configuration.disable_knockback","disable knockback");
        }
    }
}
