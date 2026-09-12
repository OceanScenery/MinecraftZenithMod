package com.oceanscenery.zenith.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.oceanscenery.zenith.registry.ZenithDamageType;
import com.oceanscenery.zenith.util.ConfigUtil;
import com.oceanscenery.zenith.util.DamageHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = LivingEntity.class,priority = 100000)
public class LivingEntityMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true,index = 2)
    private float modifyHurt(float value, @Local(argsOnly = true,index = 1) DamageSource source){
        LivingEntity livingVictim=(LivingEntity) (Object)this;
        if(source.is(ZenithDamageType.ZENITH) || source.is(ZenithDamageType.ZENITH_KNOCKBACK)){
            float expectedDamage=value+livingVictim.getMaxHealth()*ConfigUtil.healthPercentage();
            DamageHandler.addLivingVictim(livingVictim,expectedDamage);
            return expectedDamage;
        }
        return value;
    }
}
