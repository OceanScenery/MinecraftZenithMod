package com.oceanscenery.zenith.mixin;

import com.oceanscenery.zenith.zenith_class.entity.ZenithProjectile;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Entity.class, priority = Integer.MAX_VALUE)
public class EntityMixin {
    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void onSetRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof ZenithProjectile zenithProjectile) {
            if (!zenithProjectile.shouldRemove()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof ZenithProjectile zenithProjectile) {
            if (!zenithProjectile.shouldRemove()) {
                ci.cancel();
            }
        }
    }
}
