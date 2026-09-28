package com.oceanscenery.zenith.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.oceanscenery.zenith.mod_class.ZenithDamageSource;
import com.oceanscenery.zenith.util.AttachmentUtil;
import com.oceanscenery.zenith.util.ConfigUtil;
import com.oceanscenery.zenith.util.DamageHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 100000)
public class LivingEntityMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, name = "damage")
    private float modifyHurt(float damage, @Local(argsOnly = true, name = "source") DamageSource source) {
        LivingEntity livingVictim = (LivingEntity) (Object) this;
        if (source instanceof ZenithDamageSource zenithSource) {
            if (!AttachmentUtil.checkCanAttack(source.getEntity(), livingVictim)) {
                return 0f;
            }
            float expectedDamage = damage + livingVictim.getMaxHealth() * ConfigUtil.healthPercentage();
            float markedDamage = zenithSource.getIntendedDamage() + livingVictim.getMaxHealth() * ConfigUtil.healthPercentage();
            DamageHandler.addLivingVictim(livingVictim, markedDamage);
            return expectedDamage;
        }
        return damage;
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onHurt(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (source instanceof ZenithDamageSource) {
            LivingEntity livingVictim = (LivingEntity) (Object) this;
            if (!AttachmentUtil.checkCanAttack(source.getEntity(), livingVictim)) {
                cir.setReturnValue(false);
            }
        }
    }
}
