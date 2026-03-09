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
            add("the_zenith_sword.configuration.enable_bypass_invulnerable","比较真实的伤害(不推荐)");
            add("the_zenith_sword.configuration.render","渲染设置");
            add("the_zenith_sword.configuration.render_offset","剑轨渲染偏移(向下一格)");
            add("the_zenith_sword.configuration.section.the.zenith.sword.client.toml","客户端设置");
            add("the_zenith_sword.configuration.enable_bypass_invulnerable.tooltip","是否使用无视减伤的伤害(缺乏伤害效果,不稳定,不推荐)");
            add("the_zenith_sword.configuration.section.the.zenith.sword.server.toml","服务端设置");
            add("the_zenith_sword.configuration.ensured_damage_for_player.tooltip","设置对玩家的保底伤害");
            add("the_zenith_sword.configuration.ranged_damage_factor.tooltip","设置远程伤害与近战伤害的比例");
            add("the_zenith_sword.configuration.section.the.zenith.sword.server.toml.title","服务端设置");
            add("the_zenith_sword.configuration.title","模组设置");
            add("the_zenith_sword.configuration.ensured_damage_for_non_player.tooltip","设置对非玩家生物的保底伤害");
            add("the_zenith_sword.configuration.default_damage.tooltip","设置物品的默认攻击属性");
            add("the_zenith_sword.configuration.sort_farest.tooltip","设置弹射物索敌是否优先为远处敌人");
            add("the_zenith_sword.configuration.disable_knockback.tooltip","是否禁用击退");
            add("the_zenith_sword.configuration.render_offset.tooltip","将剑的轨迹在第一人称下的渲染向下偏移一格,以凸显轨道的弧形");
            add("the_zenith_sword.configuration.trail_angle","剑轨角度");
            add("the_zenith_sword.configuration.trail_angle.tooltip","决定剑的拖尾占总飞行轨道的比例(中心角)");
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
            add("the_zenith_sword.configuration.enable_bypass_invulnerable","use damage type that bypasses most damage reduction");
            add("the_zenith_sword.configuration.render","render settings");
            add("the_zenith_sword.configuration.render_offset","trail offset(1 block down)");
            add("the_zenith_sword.configuration.trail_angle","trail_angle");
        }
    }
}
