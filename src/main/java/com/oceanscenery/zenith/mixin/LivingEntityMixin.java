package com.oceanscenery.zenith.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import com.oceanscenery.zenith.util.ConfigUtil;
import com.oceanscenery.zenith.util.DamageHandler;
import com.oceanscenery.zenith.zenith_class.ZenithDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 150000)
public abstract class LivingEntityMixin {
    @WrapWithCondition(
            method = "hurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"
            )
    )
    private boolean hurt(LivingEntity instance, double vec31, double v, double p_147241_,DamageSource source){
        return !(source.is(ZenithDamageTypes.ZENITH));
    }
}
