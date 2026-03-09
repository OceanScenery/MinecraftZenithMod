package com.oceanscenery.zenith;

import com.mojang.logging.LogUtils;
import com.oceanscenery.zenith.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;

@Mod(TheZenithMod.MOD_ID)
public class TheZenithMod {
    public static final String MOD_ID = "the_zenith_sword";

    public static final Logger LOGGER= LogUtils.getLogger();

    public TheZenithMod(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, ModConfigs.CONFIG);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTab.CREATIVE_TAB.register(modEventBus);
        ModEntity.ENTITIES.register(modEventBus);
        ModEntityDataSerializer.MOD_ENTITY_DATA_SERIALIZER.register(modEventBus);
    }
}