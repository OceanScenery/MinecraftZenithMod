package com.oceanscenery.zenith;

import com.oceanscenery.zenith.registry.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(TheZenithMod.MOD_ID)
public class TheZenithMod {
    public static final String MOD_ID = "the_zenith_sword";

    public TheZenithMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ModConfigs.CONFIG);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTab.CREATIVE_TAB.register(modEventBus);
        ModEntity.ENTITIES.register(modEventBus);
        ModEntityDataSerializer.MOD_ENTITY_DATA_SERIALIZER.register(modEventBus);
    }
}