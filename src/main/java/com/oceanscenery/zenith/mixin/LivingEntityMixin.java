package com.oceanscenery.zenith.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LivingEntity.class, priority = 150000)
public abstract class LivingEntityMixin {
    @WrapWithCondition(
        method = "hurt",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"
        )
    )
    private boolean hurt(LivingEntity instance, double vec31, double v, double p_147241_, DamageSource source) {
        return !(source.is(ZenithDamageTypes.ZENITH));
    }
}
