package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.registry.ZenithConfigs;

public class ConfigUtil {
    public static float healthPercentage(){
        return ZenithConfigs.ZENITH_CONFIG.percentage_damage_factor.get().floatValue();
    }
}
