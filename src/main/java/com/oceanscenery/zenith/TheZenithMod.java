package com.oceanscenery.zenith;

import com.mojang.logging.LogUtils;
import com.oceanscenery.agent.ZenithAgentCallback;
import com.oceanscenery.agent.ZenithAgentInstaller;
import com.oceanscenery.zenith.registry.*;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;

@Mod(TheZenithMod.MOD_ID)
public class TheZenithMod {
    public static boolean TERRA_LOADED = false;
    public static final String MOD_ID = "the_zenith_sword";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public TheZenithMod(FMLJavaModLoadingContext context) {
        TERRA_LOADED = ModList.get().isLoaded("confluence");
        MixinBootstrap.init();
        ZenithAgentCallback.preload();
        ZenithAgentInstaller.install();
        context.registerConfig(ModConfig.Type.SERVER, ZenithConfigs.CONFIG);
        context.registerConfig(ModConfig.Type.CLIENT, ZenithConfigs.CLIENT_CONFIG);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TheZenithModClient.register(context));
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
