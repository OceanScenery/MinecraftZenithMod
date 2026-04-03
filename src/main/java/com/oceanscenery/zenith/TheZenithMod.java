package com.oceanscenery.zenith;

import com.mojang.logging.LogUtils;
import com.oceanscenery.zenith.registry.*;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TheZenithMod.MOD_ID)
public class TheZenithMod {
    public static final String MOD_ID = "the_zenith_sword";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static boolean TERRA_LOADED= ModList.get().isLoaded("confluence");

    public TheZenithMod(FMLJavaModLoadingContext context){
        MixinBootstrap.init();
        context.registerConfig(ModConfig.Type.SERVER,ZenithConfigs.CONFIG);
        if(FMLEnvironment.dist == Dist.CLIENT){
            context.registerConfig(ModConfig.Type.CLIENT,ZenithConfigs.CLIENT_CONFIG);
        }
        ZenithItems.ITEMS.register(context.getModEventBus());
        ZenithEntityDataSerializers.ENTITY_DATA_SERIALIZERS.register(context.getModEventBus());
        ZenithEntities.ENTITIES.register(context.getModEventBus());
        ZenithCreativeModeTabs.CREATIVE_MODE_TABS.register(context.getModEventBus());
        context.getModEventBus().addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ZenithNetworkHandler.register();
            LOGGER.info("网络系统初始化完成");
        });
    }
}
