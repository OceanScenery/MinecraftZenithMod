package com.oceanscenery.zenith.hooks;

import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.mixin.EntityAccessor;
import com.oceanscenery.zenith.mixin.LivingEntityAccessor;
import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import com.oceanscenery.zenith.util.ConfigUtil;
import com.oceanscenery.zenith.util.DamageHandler;
import com.oceanscenery.zenith.util.StaticCollections;
import com.oceanscenery.zenith.zenith_class.ZenithDamageSource;
import com.oceanscenery.zenith.zenith_class.capabilities.IZenithDamageMark;
import com.oceanscenery.zenith.zenith_class.capabilities.IZenithHealthMark;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.function.Function;

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
            if (CapabilitiesHandler.hitByZenith(entity)) {
                IZenithHealthMark mark = CapabilitiesHandler.getHealth(entity);
                if (mark == null || !mark.valid(entity.level())) {
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
            if (!zenithSource.canAttack(self)) {
                return 0.0f;
            }
            if (zenithSource.hasAttached(self)) {
                return damage;
            }
            zenithSource.markEntity(self);
            float extraDamage = Optional.ofNullable(CapabilitiesHandler.getMark(self))
                .map(IZenithDamageMark::getMap)
                .map(map -> map.remove(source.getEntity().getUUID())).orElse(0f);

            float finalDamage = Math.max(damage, zenithSource.getIntendedDamage()) + ConfigUtil.healthPercentage() * self.getMaxHealth();
            DamageHandler.addLivingVictim(self, finalDamage, extraDamage);
            var health = CapabilitiesHandler.getHealth(self);
            if (health != null && !health.valid(self.level())) {
                CapabilitiesHandler.setHealth(self, self.getHealth());
            }
            return finalDamage + extraDamage;
        } catch (Exception e) {
            return damage;
        }
    }

    public static boolean shouldHurt(DamageSource source, LivingEntity self) {
        if (source instanceof ZenithDamageSource zenithSource) {
            return zenithSource.canAttack(self);
        }
        return true;
    }

    public static boolean modifyIsDeadOrDying(boolean origin, LivingEntity self) {
        if (ACTIVE2.get()) {
            return origin;
        }
        ACTIVE2.set(true);
        try {
            if (CapabilitiesHandler.hitByZenith(self)) {
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
        if (value <= self.getHealth() && CapabilitiesHandler.hitByZenith(self)) {
            CapabilitiesHandler.setHealth(self, value);
        }
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
        if (!CapabilitiesHandler.hitByZenith(living) || receiver != self) {
            return;
        }
        if (origin <= living.getHealth()) {
            CapabilitiesHandler.setHealth(living, origin);
        }
        return;
    }

    public static boolean shouldDoDeathAct(Object mustEntity) {
        if (!(mustEntity instanceof LivingEntity living)) {
            return false;
        }
        if (living.level().isClientSide()) {
            return false;
        }
        if (CapabilitiesHandler.getHealth(living) != null
            && CapabilitiesHandler.getHealth(living).valid(living.level())
            && CapabilitiesHandler.getHealth(living).value() <= 0f
            && CapabilitiesHandler.hitByZenith(living)) {
            return true;
        }
        return false;
    }

    private static boolean intervene(Object self, int minDeathTime) {
        if (!(self instanceof LivingEntity living)) {
            return false;
        }
        if (!shouldDoDeathAct(self) || living.deathTime < minDeathTime) {
            return false;
        }
        return true;
    }

    public static void onTickDeath(Object self) {
        if (self instanceof LivingEntity living) {
            if (!shouldDoDeathAct(self)) {
                return;
            }
            if (CapabilitiesHandler.hitByZenith(living)) {
                int currDeathTicks = 0;
                if (!StaticCollections.tickDeathList.containsKey(living)) {
                    //防止deathTime修改到很小的值
                    currDeathTicks = Math.max(0, living.deathTime);
                    StaticCollections.tickDeathList.put(living, currDeathTicks);
                } else {
                    //把deathTime和自增前的值比较，这样如果方法内部正常自增的话就不会错误升高deathTime
                    currDeathTicks = StaticCollections.tickDeathList.get(living);
                    living.deathTime = Math.max(living.deathTime, currDeathTicks);
                    ++currDeathTicks;
                    StaticCollections.tickDeathList.replace(living, currDeathTicks);
                }
            } else {
                StaticCollections.tickDeathList.remove(living);
            }
        }
    }

    public static int jumpBranch(int origin, Object self, int forced, int minDeathTime) {
        return intervene(self, minDeathTime) ? forced : origin;
    }

    public static int compareBranch(int a, int b, Object self, int forced, int minDeathTime) {
        return intervene(self, minDeathTime) ? forced : Integer.compare(a, b);
    }

    public static int nullBranch(Object ref, Object self, int forced, int minDeathTime) {
        return intervene(self, minDeathTime) ? forced : (ref == null ? 1 : 0);
    }

    public static int refCompareBranch(Object a, Object b, Object self, int forced, int minDeathTime) {
        return intervene(self, minDeathTime) ? forced : (a == b ? 1 : 0);
    }

    public static int switchBranch(int key, Object self, int forcedKey, int minDeathTime) {
        return intervene(self, minDeathTime) ? forcedKey : key;
    }

    public static int deathTimeModify(int origin, Object entity) {
        if (!(entity instanceof LivingEntity living)) {
            return origin;
        }
        if (!shouldDoDeathAct(entity)) {
            return origin;
        }
        return Math.max(origin, living.deathTime);
    }

    public static void onRemoveStart(Object mustEntity) {
        if (mustEntity instanceof LivingEntity entity && CapabilitiesHandler.hitByZenith(entity)) {
            if (entity instanceof Player) {
                return;
            }
            StaticCollections.removed.add(entity);
        }
    }

    public static boolean modifyIsRemoved(boolean origin, Object mustEntity) {
        if (mustEntity instanceof LivingEntity entity) {
            if (StaticCollections.removed.contains(entity)) {
                return true;
            }
        }
        return origin;
    }

    public static boolean shouldIgnoreKnockBack(DamageSource source, Object entity) {
        if (!(entity instanceof Entity)) {
            return false;
        }
        if (source.is(ZenithDamageTypes.ZENITH)) {
            return true;
        }
        return false;
    }
}
