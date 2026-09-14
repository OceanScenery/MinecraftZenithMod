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

@Mixin(value = LivingEntity.class,priority = 150000)
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

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true,index = 2)
    private float modifyHurt(float damage,@Local(argsOnly = true,index = 1) DamageSource source){
        LivingEntity livingVictim=(LivingEntity) (Object)this;
        if(source instanceof ZenithDamageSource zenithSource){
            if(!CapabilitiesHandler.checkCanAttack(source.getEntity(),livingVictim)){
                return 0f;
            }
            float expectedDamage=damage+livingVictim.getMaxHealth()*ConfigUtil.healthPercentage();
            float markedDamage= zenithSource.getIntendedDamage()+livingVictim.getMaxHealth()*ConfigUtil.healthPercentage();
            DamageHandler.addLivingVictim(livingVictim,markedDamage);
            return expectedDamage;
        }
        return damage;
    }

    @Inject(method = "hurt",at = @At("HEAD"),cancellable = true)
    private void hurt(DamageSource p_21016_, float p_21017_, CallbackInfoReturnable<Boolean> cir){
        if(p_21016_ instanceof ZenithDamageSource zenithSource){
            LivingEntity livingVictim=(LivingEntity) (Object)this;
            if(!CapabilitiesHandler.checkCanAttack(zenithSource.getEntity(),livingVictim)){
                cir.setReturnValue(false);
            }
        }
    }
}
