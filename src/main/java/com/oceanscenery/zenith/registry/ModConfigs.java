package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.mod_class.config.ZenithConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ModConfigs {
    public static final ModConfigSpec CONFIG;
    public static final ZenithConfig ZENITH_CONFIG;

    // 静态初始化块，用于构建配置
    static {
        final Pair<ZenithConfig,ModConfigSpec> specPair=new ModConfigSpec.Builder().configure(ZenithConfig::new);
        ZENITH_CONFIG=specPair.getLeft();
        CONFIG= specPair.getRight();
    }

    public static float getPDamageP(){
        return ZENITH_CONFIG.ensured_damage_for_player.get().floatValue();
    }

    public static float getPDamageNP(){
        return ZENITH_CONFIG.ensured_damage_for_non_player.get().floatValue();
    }

    public static double getDefaultDamage(){
        return ZENITH_CONFIG.default_damage.get();
    }

    public static double getFactor(){
        return ZENITH_CONFIG.ranged_damage_factor.get();
    }
}
