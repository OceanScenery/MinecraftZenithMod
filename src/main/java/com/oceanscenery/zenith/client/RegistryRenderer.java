package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegistryRenderer {
    @SubscribeEvent
    public static void onRegister(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(ZenithEntities.ZENITH_PROJECTILE.get(), ZenithProjectileRenderer::new);
    }
}
