package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.mod_class.data_component.Distance;
import com.oceanscenery.zenith.mod_class.data_component.LastUseTime;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZenithDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS=DeferredRegister.createDataComponents(
            Registries.DATA_COMPONENT_TYPE,
            TheZenithMod.MOD_ID
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LastUseTime>> LAST_USE_TIME=DATA_COMPONENTS.registerComponentType(
            "last_use_time",
            lastUseTimeBuilder -> lastUseTimeBuilder.persistent(LastUseTime.CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Distance>> DISTANCE=DATA_COMPONENTS.registerComponentType(
            "distance",
            distanceBuilder -> distanceBuilder.persistent(Distance.CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<AttackMode>> ATTACK_MODE=DATA_COMPONENTS.registerComponentType(
            "attack_mode",
            attackModeBuilder -> attackModeBuilder.persistent(AttackMode.CODEC)
    );
}
