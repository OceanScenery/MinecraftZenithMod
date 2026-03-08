package com.oceanscenery.zenith;

import com.oceanscenery.zenith.registry.ModConfigs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value=TheZenithMod.MOD_ID,dist= Dist.CLIENT)
public class TheZenithModClient {
    public TheZenithModClient(ModContainer modContainer){
        modContainer.registerConfig(ModConfig.Type.CLIENT, ModConfigs.CLIENT_CONFIG);
        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (mc, parent) -> new ConfigurationScreen(modContainer,parent)
        );
    }
}
