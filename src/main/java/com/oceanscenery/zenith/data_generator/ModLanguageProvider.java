package com.oceanscenery.zenith.data_generator;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithEntities;
import com.oceanscenery.zenith.registry.ZenithItems;
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
            add(ZenithItems.ZENITH.get(),"天顶剑");
            add(ZenithEntities.ZENITH_PROJECTILE.get(),"天顶剑射弹");
            add("the_zenith_sword.configuration.attack_damage","伤害设置");
            add("the_zenith_sword.configuration.ensured_damage_for_player","对玩家保底伤害比例");
            add("the_zenith_sword.configuration.ensured_damage_for_non_player","对非玩家保底伤害比例");
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
            add("the_zenith_sword.packet.distance","默认索敌距离");
            add("the_zenith_sword.item.distance.tooltip_pre","当前物品的默认索敌距离");
            add("the_zenith_sword.item.distance.tooltip_post","在背包中单独右键物品以改变默认索敌距离");
            add("the_zenith_sword.packet.attack_mode","攻击模式(实体类型)");
            add("the_zenith_sword.item.attack_mode.tooltip_pre","攻击模式");
            add("the_zenith_sword.item.attack_mode.tooltip_post","在背包中Ctrl+右键物品以改变默认攻击模式");
            add("the_zenith_sword.packet.attack_player_mode","伤害玩家");
            add("the_zenith_sword.item.attack_player_mode.tooltip.pre","伤害玩家");
            add("the_zenith_sword.item.attack_player_mode.tooltip.post","在背包中Shift+右键以切换是否攻击玩家");
            add("the_zenith_sword.configuration.enable_attack_item.tooltip","是否在剑的\"all\"模式下攻击掉落物或经验球");
            add("the_zenith_sword.configuration.enable_attack_item","攻击掉落物与经验球");
            add("the_zenith_sword.configuration.interaction","交互设置");
            add("the_zenith_sword.configuration.block_interaction","屏蔽交互");
            add("the_zenith_sword.configuration.block_interaction.tooltip","手持天顶剑时,屏蔽除攻击外的右键交互");
            add("the_zenith_sword.configuration.enable_terra_damage_modifier.tooltip","装有汇流来世时,大幅度提高天顶剑攻击力");
            add("the_zenith_sword.configuration.enable_terra_damage_modifier","允许汇流来世攻击力调整");
        }
        if(locale.equals("en_us")){
            add(ZenithItems.ZENITH.get(),"Zenith Sword");
            add(ZenithEntities.ZENITH_PROJECTILE.get(),"ZenithProjectile");
            add("the_zenith_sword.configuration.attack_damage","damage settings");
            add("the_zenith_sword.configuration.ensured_damage_for_player","minimum damage ratio for player (0.0-1.0), damage will be raised to this ratio of original damage if lower");
            add("the_zenith_sword.configuration.ensured_damage_for_non_player","minimum damage ratio for non-player (0.0-1.0), damage will be raised to this ratio of original damage if lower");
            add("the_zenith_sword.configuration.default_damage","default damage");
            add("the_zenith_sword.configuration.ranged_damage_factor","ranged damage factor");
            add("the_zenith_sword.configuration.sort_farest","sort farest enemy first");
            add("the_zenith_sword.configuration.disable_knockback","disable knockback");
            add("the_zenith_sword.configuration.enable_bypass_invulnerable","use damage type that bypasses most damage reduction");
            add("the_zenith_sword.configuration.render","render settings");
            add("the_zenith_sword.configuration.render_offset","trail offset(1 block down)");
            add("the_zenith_sword.configuration.trail_angle","trail_angle");
            add("the_zenith_sword.packet.distance","default enemy targeting range");
            add("the_zenith_sword.item.distance.tooltip_pre","default targeting range for the current item");
            add("the_zenith_sword.item.distance.tooltip_post","right click the item in the inventory to change the default targeting range.");
            add("the_zenith_sword.packet.attack_mode","attack mode");
            add("the_zenith_sword.item.attack_mode.tooltip_pre","attack mode");
            add("the_zenith_sword.item.attack_mode.tooltip_post","right click(with Ctrl) the item in the inventory to change the attack mode.");
            add("the_zenith_sword.packet.attack_player_mode","hurt player");
            add("the_zenith_sword.item.attack_player_mode.tooltip.pre","hurt player");
            add("the_zenith_sword.item.attack_player_mode.tooltip.post","right click(with Shift) the item in the inventory to change whether to hurt player");
            add("the_zenith_sword.configuration.enable_attack_item.tooltip","whether to attack item or xp orb in \"all\" mode");
            add("the_zenith_sword.configuration.enable_attack_item","attack item/xp orb");
            add("the_zenith_sword.configuration.interaction","Interaction");
            add("the_zenith_sword.configuration.block_interaction","Block Interaction");
            add("the_zenith_sword.configuration.block_interaction.tooltip","When holding a Zenith Sword, right-click interactions (except for attacks) will be blocked");
            add("the_zenith_sword.configuration.enable_terra_damage_modifier","Enable Confluence:Otherworld damage modifier");
            add("the_zenith_sword.configuration.enable_terra_damage_modifier.tooltip","When Confluence:Otherworld is installed, significantly increase Zenith Sword attack power");
        }
    }
}
