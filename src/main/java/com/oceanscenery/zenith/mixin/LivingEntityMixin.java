package com.oceanscenery.zenith.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.oceanscenery.zenith.mod_class.ZenithDamageSource;
import com.oceanscenery.zenith.util.AttachmentUtil;
import com.oceanscenery.zenith.util.ConfigUtil;
import com.oceanscenery.zenith.util.DamageHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class,priority = 100000)
public class LivingEntityMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true,index = 2)
    private float modifyHurt(float value, @Local(argsOnly = true,index = 1) DamageSource source){
        LivingEntity livingVictim=(LivingEntity) (Object)this;
        if(source instanceof ZenithDamageSource zenithSource){
            if(!AttachmentUtil.checkCanAttack(source.getEntity(),livingVictim)){
                return 0f;
            }
            float expectedDamage=value+livingVictim.getMaxHealth()*ConfigUtil.healthPercentage();
            float markedDamage= zenithSource.getIntendedDamage()+livingVictim.getMaxHealth()*ConfigUtil.healthPercentage();
            DamageHandler.addLivingVictim(livingVictim,markedDamage);
            return expectedDamage;
        }
        return value;
    }

    @Inject(method = "hurt",at = @At("HEAD"),cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
        if(source instanceof ZenithDamageSource){
            LivingEntity livingVictim=(LivingEntity) (Object)this;
            if(!AttachmentUtil.checkCanAttack(source.getEntity(),livingVictim)){
                cir.setReturnValue(false);
            }
        }
    }
}
