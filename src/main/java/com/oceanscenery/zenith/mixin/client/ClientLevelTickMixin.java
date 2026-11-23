package com.oceanscenery.zenith.mixin.client;

import com.oceanscenery.zenith.mixin_extension.IZenithEntityExtension;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelTickMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void zenith$skipTick(Entity entity, CallbackInfo ci) {
        if (IZenithEntityExtension.of(entity).zenith$isAllReturn()) {
            ci.cancel();
        }
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void zenith$skipPassengerTick(Entity vehicle, Entity passenger, CallbackInfo ci) {
        if (IZenithEntityExtension.of(passenger).zenith$isAllReturn()) {
            ci.cancel();
        }
    }
}
