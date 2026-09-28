package com.oceanscenery.zenith.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, priority = Integer.MAX_VALUE)
public class EntityMixin {
    @Shadow
    private Entity.RemovalReason removalReason;

    @Inject(method = "isRemoved", at = @At("HEAD"), cancellable = true)
    private void onCheckRemoved(CallbackInfoReturnable<Boolean> cir) {
        if (this.removalReason != null) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void onSetRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        this.removalReason = reason;
    }
}
