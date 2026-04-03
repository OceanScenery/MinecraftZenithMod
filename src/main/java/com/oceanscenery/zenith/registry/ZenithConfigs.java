package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.zenith_class.config.ZenithClientConfig;
import com.oceanscenery.zenith.zenith_class.config.ZenithConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ZenithConfigs {
    public static final ForgeConfigSpec CONFIG,CLIENT_CONFIG;
    public static final ZenithConfig ZENITH_CONFIG;
    public static final ZenithClientConfig ZENITH_CLIENT_CONFIG;

    static {
        final Pair<ZenithConfig,ForgeConfigSpec> specPair=new ForgeConfigSpec.Builder().configure(ZenithConfig::new);
        ZENITH_CONFIG=specPair.getLeft();
        CONFIG= specPair.getRight();
        final Pair<ZenithClientConfig,ForgeConfigSpec> clientSpecPair=new ForgeConfigSpec.Builder().configure(ZenithClientConfig::new);
        ZENITH_CLIENT_CONFIG=clientSpecPair.getLeft();
        CLIENT_CONFIG=clientSpecPair.getRight();
    }

    public static float getEnsuredDamageForPlayer(){
        return ZENITH_CONFIG.ensured_damage_for_player.get().floatValue();
    }

    public static float getEnsuredDamageForNonPlayer(){
        return ZENITH_CONFIG.ensured_damage_for_non_player.get().floatValue();
    }

    public static double getDefaultDamage(){
        return ZENITH_CONFIG.default_damage.get();
    }

    public static double getRangedFactor(){
        return ZENITH_CONFIG.ranged_damage_factor.get();
    }
}
