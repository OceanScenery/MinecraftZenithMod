package com.oceanscenery.zenith.class_bytecode.common;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mixin.EntityAccessor;
import com.oceanscenery.zenith.mixin.LivingEntityAccessor;
import com.oceanscenery.zenith.mod_class.ZenithDamageSource;
import com.oceanscenery.zenith.mod_class.attachment.ZenithHealthMark;
import com.oceanscenery.zenith.util.AttachmentUtil;
import com.oceanscenery.zenith.util.ConfigUtil;
import com.oceanscenery.zenith.util.DamageHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class Hooks {
    private static final ThreadLocal<Boolean> ACTIVE1 = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> ACTIVE2 = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> ACTIVE3 = ThreadLocal.withInitial(() -> false);

    private Hooks() {
    }

    public static float modifyGetHealth(float origin, LivingEntity entity) {
        if (ACTIVE1.get()) {
            return origin;
        }
        ACTIVE1.set(true);
        try {
            if (AttachmentUtil.hitByZenith(entity)) {
                ZenithHealthMark mark = AttachmentUtil.getHealth(entity);
                if (!mark.valid(entity.level())) {
                    return ((EntityAccessor) entity).getRealEntityData().get(LivingEntityAccessor.getHealthId());
                } else {
                    ((EntityAccessor) entity).getRealEntityData().set(LivingEntityAccessor.getHealthId(), mark.value());
                    return mark.value();
                }
            }
            return origin;
        } finally {
            ACTIVE1.set(false);
        }
    }

    public static float beforeHurt(DamageSource source, float damage, LivingEntity self) {
        try {
            if (!(source instanceof ZenithDamageSource zenithSource)) {
                return damage;
            }
            if (!AttachmentUtil.checkCanAttack(source.getEntity(), self)) {
                return 0.0f;
            }
            if (zenithSource.hasAttached(self)) {
                return damage;
            }
            zenithSource.markEntity(self);
            float extraDamage = Optional.ofNullable(
                AttachmentUtil.getMark(self).getMap().remove(source.getEntity().getUUID())
            ).orElse(0f);

            if (!AttachmentUtil.getHealth(self).valid(self.level())) {
                AttachmentUtil.setHealth(self, self.getHealth());
            }
            float finalDamage = Math.max(damage, zenithSource.getIntendedDamage()) + ConfigUtil.healthPercentage() * self.getMaxHealth();
            DamageHandler.addLivingVictim(self, finalDamage, extraDamage);
            return finalDamage + extraDamage;
        } catch (Exception e) {
            TheZenithMod.LOGGER.error("Exception at method hurtServer", e);
            return damage;
        }
    }

    public static boolean shouldHurt(DamageSource source, LivingEntity self) {
        if (source instanceof ZenithDamageSource) {
            return AttachmentUtil.checkCanAttack(source.getEntity(), self);
        }
        return true;
    }

    public static boolean modifyIsDeadOrDying(boolean origin, LivingEntity self) {
        if (ACTIVE2.get()) {
            return origin;
        }
        ACTIVE2.set(true);
        try {
            if (AttachmentUtil.hitByZenith(self)) {
                if (self.getHealth() <= 0f) {
                    return true;
                }
            }
            return origin;
        } finally {
            ACTIVE2.set(false);
        }
    }

    public static void beforeSetHealth(float value, LivingEntity self) {
        ((EntityAccessor) self).getRealEntityData().set(LivingEntityAccessor.getHealthId(), value);
    }

    public static boolean modifyIsAlive(boolean origin, LivingEntity self) {
        if (ACTIVE3.get()) {
            return origin;
        }
        ACTIVE3.set(true);
        try {
            if (self.isDeadOrDying()) {
                return false;
            }
            return origin;
        } finally {
            ACTIVE3.set(false);
        }
    }

    public static void beforeSetHealthInActHurt(Object receiver, float origin, Object self) {
        if (!(self instanceof LivingEntity living)) {
            return;
        }
        if (!AttachmentUtil.hitByZenith(living) || receiver != self) {
            return;
        }
        if (origin <= living.getHealth()) {
            AttachmentUtil.setHealth(living, origin);
        }
    }
}
