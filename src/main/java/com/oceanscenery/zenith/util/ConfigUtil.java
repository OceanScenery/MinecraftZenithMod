package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

public class ConfigUtil {
    public static float healthPercentage() {
        return ZenithConfigs.ZENITH_CONFIG.percentage_damage_factor.get().floatValue();
    }

    public static boolean shouldKnock() {
        return !ZenithConfigs.ZENITH_CONFIG.disable_knockback.get();
    }

    public static double ensuredDamage(boolean forPlayer) {
        return forPlayer ?
            ZenithConfigs.ZENITH_CONFIG.ensured_damage_for_player.get()
            : ZenithConfigs.ZENITH_CONFIG.ensured_damage_for_non_player.get();
    }

    public static double defaultDamage() {
        return ZenithConfigs.ZENITH_CONFIG.default_damage.get();
    }

    public static double attackSpeed(boolean shadow) {
        if (!shadow) {
            return ModList.get().isLoaded("bettercombat") ? -2.4 : 30;
        } else {
            return ModList.get().isLoaded("bettercombat") ? 6 : 30;
        }
    }

    public static double rangedFactor() {
        return ZenithConfigs.ZENITH_CONFIG.ranged_damage_factor.get();
    }

    public static boolean sortFarest() {
        return ZenithConfigs.ZENITH_CONFIG.sort_farest.get();
    }

    public static boolean bypass() {
        return ZenithConfigs.ZENITH_CONFIG.enable_bypass_invulnerable.get();
    }

    public static boolean attackItem() {
        return ZenithConfigs.ZENITH_CONFIG.enable_attack_item.get();
    }

    public static double percentage() {
        return ZenithConfigs.ZENITH_CONFIG.percentage_damage_factor.get();
    }

    public static boolean terraModifier() {
        return ZenithConfigs.ZENITH_CONFIG.enable_terra_damage_modifier.get();
    }

    public static boolean forceRemoval() {
        return ZenithConfigs.ZENITH_CONFIG.forceRemoval.get();
    }

    public static void rethrowUnlessRemove(RuntimeException failure) {
        try {
            if (ZenithConfigs.CONFIG.isLoaded() && forceRemoval()) {
                return;
            }
        } catch (IllegalStateException | NullPointerException ignored) {
        }
        throw failure;
    }

    public static void redirectDamageAction(LivingEntity livingVictim, float prevCheckedHealth, float postCheckedHealth, float damage, DamageSource source) {
        if (forceRemoval()) {
            DamageHandler.decideDeath(livingVictim, prevCheckedHealth, postCheckedHealth, damage, source);
        } else {
            CapabilitiesHandler.addDamageMark(livingVictim, prevCheckedHealth, postCheckedHealth, damage, source);
        }
    }
}
