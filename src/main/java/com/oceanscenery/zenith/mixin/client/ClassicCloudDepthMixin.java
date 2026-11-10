package com.oceanscenery.zenith.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.oceanscenery.zenith.client.RenderZenithTrail;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher.PreparedFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class ClassicCloudDepthMixin {
    @Unique
    private RenderPass zenith$cloudContinuation;

    @WrapMethod(method = "executeClassicTransparency")
    private void zenith$closeCloudContinuation(ChunkSectionsToRender chunkSectionsToRender, PreparedFrame featureFrame
        , RenderPass renderPass, Operation<Void> original) {
        try {
            original.call(chunkSectionsToRender, featureFrame, renderPass);
        } finally {
            if (zenith$cloudContinuation != null) {
                zenith$cloudContinuation.close();
                zenith$cloudContinuation = null;
            }
        }
    }

    @Inject(method = "executeClassicTransparency",
        slice = @Slice(from = @At(value = "NEW",
            target = "net/neoforged/neoforge/client/event/RenderLevelStageEvent$AfterTranslucentParticles")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V",
            ordinal = 0, shift = At.Shift.AFTER))
    private void zenith$copyDepthBeforeClouds(ChunkSectionsToRender sections, PreparedFrame features, RenderPass pass
        , CallbackInfo ci, @Local(name = "shouldRenderClouds") boolean shouldRenderClouds
        , @Local(argsOnly = true, name = "renderPass") LocalRef<RenderPass> activePass) {
        if (!shouldRenderClouds || !RenderZenithTrail.hasPendingTrails()) {
            return;
        }
        pass.close();
        zenith$cloudContinuation = RenderZenithTrail.resumeClassicPassWithDepthSnapshot();
        activePass.set(zenith$cloudContinuation);
    }
}
