package com.oceanscenery.zenith.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.mixin_extension.IZenithLivingEntityExtension;
import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import com.oceanscenery.zenith.util.ConfigUtil;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithSlot;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LivingEntity.class, priority = 150000)
public abstract class LivingEntityMixin implements IZenithLivingEntityExtension {
    @Unique
    private int zenith$deathTime = 0;
    @Unique
    private boolean zenith$dead = false;

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

    @ModifyReturnValue(
        method = "getAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D",
        at = @At("RETURN")
    )
    private double zenith$getAttributeValue(double original, @Local(argsOnly = true, ordinal = 0) Attribute attribute) {
        if (!((LivingEntity) (Object) this instanceof Player player)) {
            return original;
        }
        if (attribute == Attributes.ATTACK_DAMAGE) {
            IZenithSlot slot = CapabilitiesHandler.getSlot(player);
            if (slot != null && slot.open()) {
                return ConfigUtil.defaultDamage() + original;
            }
        } else if (attribute == Attributes.ATTACK_SPEED) {
            IZenithSlot slot = CapabilitiesHandler.getSlot(player);
            if (slot != null && slot.open()) {
                return Math.max(ConfigUtil.attackSpeed(true), original);
            }
        }
        return original;
    }

    @ModifyReturnValue(
        method = "getAttributeValue(Lnet/minecraft/core/Holder;)D",
        at = @At("RETURN")
    )
    private double zenith$getAttributeValue(double original, @Local(argsOnly = true, ordinal = 0) Holder<Attribute> attribute) {
        try {
            if (!((LivingEntity) (Object) this instanceof Player player)) {
                return original;
            }
            if (attribute.get() == Attributes.ATTACK_DAMAGE) {
                IZenithSlot slot = CapabilitiesHandler.getSlot(player);
                if (slot != null && slot.open()) {
                    return ConfigUtil.defaultDamage() + original;
                }
            } else if (attribute.get() == Attributes.ATTACK_SPEED) {
                IZenithSlot slot = CapabilitiesHandler.getSlot(player);
                if (slot != null && slot.open()) {
                    return Math.max(ConfigUtil.attackSpeed(true), original);
                }
            }
            return original;
        } catch (Exception ignored) {
            return original;
        }
    }

    @Override
    public int zenith$deathTime() {
        return this.zenith$deathTime;
    }

    @Override
    public void zenith$setDeathTime(int value) {
        this.zenith$deathTime = value;
    }

    @Override
    public boolean zenith$deadMark() {
        return this.zenith$dead;
    }

    @Override
    public void zenith$setDead(boolean value) {
        this.zenith$dead = value;
        if (!value) {
            this.zenith$deathTime = 0;
        }
    }
}
