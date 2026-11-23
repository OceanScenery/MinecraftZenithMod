package com.oceanscenery.zenith.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.oceanscenery.zenith.mixin_extension.IZenithEntityExtension;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void zenith$skipRender(Entity entity, double x, double y, double z, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (IZenithEntityExtension.of(entity).zenith$isAllReturn()) {
            ci.cancel();
        }
    }
}
