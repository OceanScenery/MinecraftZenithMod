package com.oceanscenery.zenith.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import com.oceanscenery.zenith.util.DamageHandler;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class,priority = 150000)
public abstract class LivingEntityMixin {
    @Accessor(value = "DATA_HEALTH_ID")
    public abstract EntityDataAccessor<Float> getDataHealthId();
    @Shadow
    protected void dropAllDeathLoot(DamageSource p_21192_){}

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

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true,index = 2)
    private float modifyHurt(float value,@Local(argsOnly = true,index = 1) DamageSource source){
        LivingEntity livingVictim=(LivingEntity) (Object)this;
        if(source.is(ZenithDamageTypes.ZENITH) || source.is(ZenithDamageTypes.ZENITH_KNOCKBACK)){
            float expectedDamage=value+livingVictim.getMaxHealth()* DamageHandler.EXTRA_DAMAGE_PERCENTAGE;
            DamageHandler.addLivingVictim(livingVictim,expectedDamage);
            return expectedDamage;
        }
        return value;
    }
}
