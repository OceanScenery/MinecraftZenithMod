package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID, value = Dist.CLIENT)
public final class RenderZenithTrail {
    private RenderZenithTrail() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!ZenithProjectileRenderer.usesStagedTrails()) {
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            ZenithProjectileRenderer.discardPendingTrails();
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            ZenithProjectileRenderer.captureDepthBeforeClouds();
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        if (Minecraft.getInstance().level == null) {
            ZenithProjectileRenderer.discardPendingTrails();
            return;
        }
        ZenithProjectileRenderer.drawTrailsAfterLevel();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ZenithProjectileRenderer.releaseTrailResources();
    }
}
