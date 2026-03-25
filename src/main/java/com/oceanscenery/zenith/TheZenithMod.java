package com.oceanscenery.zenith;

import com.mojang.logging.LogUtils;
import com.oceanscenery.zenith.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;

@Mod(TheZenithMod.MOD_ID)
public class TheZenithMod {
    public static final String MOD_ID = "the_zenith_sword";

    public static final Logger LOGGER= LogUtils.getLogger();

    public static final boolean TERRA_LOADED= ModList.get().isLoaded("confluence");

    public TheZenithMod(IEventBus modEventBus, ModContainer modContainer) {
        ZenithDataComponents.DATA_COMPONENTS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, ZenithConfigs.CONFIG);
        ZenithItems.ITEMS.register(modEventBus);
        ZenithCreativeTab.CREATIVE_TAB.register(modEventBus);
        ZenithEntities.ENTITIES.register(modEventBus);
        ZenithEntityDataSerializer.MOD_ENTITY_DATA_SERIALIZER.register(modEventBus);
        ZenithAttachment.ATTACHMENTS.register(modEventBus);
    }
}