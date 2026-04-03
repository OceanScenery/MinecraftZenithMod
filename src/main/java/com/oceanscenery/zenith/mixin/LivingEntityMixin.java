package com.oceanscenery.zenith.mixin;

import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class,priority = 1500)
public abstract class LivingEntityMixin {
    @Unique
    private DamageSource zenith$lastHurt;
    @Unique
    public DamageSource zenith$getLastHurt() {
        return zenith$lastHurt;
    }
    @Unique
    public void zenith$setLastHurt(DamageSource lastHurt) {
        this.zenith$lastHurt = lastHurt;
    }
    @Inject(method = "hurt",at = @At("HEAD"))
    public void collectSource(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
        this.zenith$setLastHurt(source);
    }
    @Inject(method = "knockback",at=@At("HEAD"), cancellable = true)
    public void onKnockback(double p_147241_, double p_147242_, double p_147243_, CallbackInfo ci){
        if(this.zenith$getLastHurt()!=null && this.zenith$getLastHurt().is(ZenithDamageTypes.ZENITH)){
            this.zenith$setLastHurt(null);
            ci.cancel();
        }
    }
}
