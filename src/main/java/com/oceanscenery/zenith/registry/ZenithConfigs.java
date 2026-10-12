package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.mod_class.config.ZenithClientConfig;
import com.oceanscenery.zenith.mod_class.config.ZenithConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ZenithConfigs {
    public static final ModConfigSpec CONFIG, CLIENT_CONFIG;
    public static final ZenithConfig ZENITH_CONFIG;
    public static final ZenithClientConfig ZENITH_CLIENT_CONFIG;

    static {
        final Pair<ZenithConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ZenithConfig::new);
        ZENITH_CONFIG = specPair.getLeft();
        CONFIG = specPair.getRight();
        final Pair<ZenithClientConfig, ModConfigSpec> clientSpecPair = new ModConfigSpec.Builder().configure(ZenithClientConfig::new);
        ZENITH_CLIENT_CONFIG = clientSpecPair.getLeft();
        CLIENT_CONFIG = clientSpecPair.getRight();
    }
}
