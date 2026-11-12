package com.oceanscenery.zenith.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.oceanscenery.zenith.client.RenderZenithTrail;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher.PreparedFrame;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class ClassicCloudDepthMixin {
    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Unique
    private RenderPass zenith$cloudContinuation;

    @WrapMethod(method = "executeClassicTransparency")
    private void zenith$closeCloudContinuation(ChunkSectionsToRender chunkSectionsToRender, PreparedFrame featureFrame
        , RenderPass renderPass, Operation<Void> original) {
        try {
            original.call(chunkSectionsToRender, featureFrame, renderPass);
        } finally {
            RenderPass continuation = zenith$cloudContinuation;
            zenith$cloudContinuation = null;
            if (continuation != null) {
                try {
                    continuation.close();
                } catch (RuntimeException | LinkageError failure) {
                    RenderZenithTrail.switchToEntityRendering("Zenith could not close the resumed cloud pass.", failure);
                }
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
        RenderPass continuation = RenderZenithTrail.resumeClassicPassWithDepthSnapshot(pass, () -> targets.main.get());
        if (continuation != pass) {
            zenith$cloudContinuation = continuation;
            activePass.set(continuation);
        }
    }
}
