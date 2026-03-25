  package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID, value = Dist.CLIENT)
public class RendererMain {
    @SubscribeEvent
    public static void RenderItem(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(ZenithEntities.ZENITH_PROJECTILE.get(),ZenithProjectileRenderer::new);
    }
}
