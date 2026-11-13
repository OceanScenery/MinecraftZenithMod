package com.oceanscenery.zenith.hooks;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.mixin.accessor.EntityAccessor;
import com.oceanscenery.zenith.mixin.accessor.LivingEntityAccessor;
import com.oceanscenery.zenith.mixin_extension.IZenithEntityExtension;
import com.oceanscenery.zenith.mixin_extension.IZenithLivingEntityExtension;
import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import com.oceanscenery.zenith.util.ConfigUtil;
import com.oceanscenery.zenith.util.DamageHandler;
import com.oceanscenery.zenith.zenith_class.ZenithDamageSource;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithDamageMark;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithHealthMark;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithHitMark;
import com.oceanscenery.zenith.zenith_class.entity.ZenithProjectile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class Hooks {
    private static final ThreadLocal<Boolean> ACTIVE1 = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> ACTIVE2 = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> ACTIVE3 = ThreadLocal.withInitial(() -> false);

    private static final Pattern DRAGON_PATTERN = Pattern.compile("^iceandfire[^:]*:[^_]*_dragon$");

    private static boolean flag = true;

    private static final Function<Entity, Optional<Integer>> SPECIAL_TIME = entity -> {
        String id = Optional.ofNullable(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()))
            .map(ResourceLocation::toString).orElse("");
        if (id.equals("minecraft:ender_dragon")) {
            return Optional.of(200);
        }
        if (id.startsWith("cataclysm:")) {
            return Optional.of(200);
        }
        if (id.contains("apollyon") && id.contains("goety")) {
            return Optional.of(560);
        }
        if (id.equals("goety:apostle")) {
            if (!flag) {
                return Optional.of(200);
            }
            try {
                Class<?> apostleClass = Class.forName("com.Polarice3.Goety.common.entities.boss.Apostle");

                Method isInNether = apostleClass.getMethod("isInNether");
                boolean inNether = (Boolean) isInNether.invoke(entity);

                Method isApollyon = apostleClass.getMethod("allTitlesApostle_1_20_1$isApollyon");
                boolean apollyon = Optional.of(isApollyon.invoke(entity))
                    .filter(Boolean.TRUE::equals).isPresent();

                if (apollyon && inNether) {
                    return Optional.of(560);
                }
                return Optional.of(200);
            } catch (Throwable t) {
                TheZenithMod.LOGGER.warn("未识别到亚波伦");
                flag = false;
                return Optional.of(200);
            }
        }
        if (DRAGON_PATTERN.matcher(id).matches()) {
            return Optional.of(12000);
        }
        if (id.contains("ender_dragon")) {
            return Optional.of(200);
        }
        return Optional.empty();
    };

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
                if (mark == null || !mark.valid(entity)) {
                    return ((EntityAccessor) entity).getRealEntityData().get(LivingEntityAccessor.getHealthId());
                } else {
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
            if (health != null && !health.valid(self)) {
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
        IZenithHealthMark mark = CapabilitiesHandler.getHealth(living);
        if (mark != null
            && mark.valid(living)
            && mark.value() <= 0f
            && CapabilitiesHandler.hitByZenith(living)) {
            return true;
        }
        return false;
    }

    private static boolean intervene(Object self, int minDeathTime) {
        if (!(self instanceof LivingEntity living)) {
            return false;
        }
        if (!shouldDoDeathAct(self)) {
            return false;
        }
        if (minDeathTime <= 0) {
            return true;
        }
        int finalMinDeathTime = SPECIAL_TIME.apply(living).orElse(minDeathTime);
        IZenithHitMark mark = CapabilitiesHandler.getHitMark(living);
        if (mark == null) {
            return true;
        }

        if (mark.endTime() <= living.level().getGameTime() + finalMinDeathTime) {
            CapabilitiesHandler.setHitByZenith(living, ignoredBool -> true, 2L * finalMinDeathTime);
        }
        if (living.deathTime < finalMinDeathTime) {
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
                if (!IZenithLivingEntityExtension.of(living).zenith$deadMark()) {
                    //防止deathTime修改到很小的值
                    currDeathTicks = Math.max(0, living.deathTime);
                    IZenithLivingEntityExtension.of(living).zenith$setDeathTime(currDeathTicks);
                    IZenithLivingEntityExtension.of(living).zenith$setDead(true);
                } else {
                    //把deathTime和自增前的值比较，这样如果方法内部正常自增的话就不会错误升高deathTime
                    currDeathTicks = IZenithLivingEntityExtension.of(living).zenith$deathTime();
                    living.deathTime = Math.max(living.deathTime, currDeathTicks);
                    ++currDeathTicks;
                    IZenithLivingEntityExtension.of(living).zenith$setDeathTime(currDeathTicks);
                }
            } else {
                IZenithLivingEntityExtension.of(living).zenith$setDead(false);
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

    public static void onRemoveEnd(Object mustEntity, Object reason) {
        try {
            if (mustEntity instanceof Entity entity && CapabilitiesHandler.hitByZenith(entity)) {
                if (entity instanceof Player || entity.level().isClientSide()) {
                    return;
                }
                if (entity instanceof ZenithProjectile) {
                    return;
                }
                if (((EntityAccessor) entity).zenith$getRemovalReason() != null || IZenithEntityExtension.of(entity).zenith$removed()) {
                    return;
                }
                if (!entity.level().isClientSide()) {
                    Entity.RemovalReason real = null;
                    if (reason instanceof Entity.RemovalReason reason1) {
                        real = reason1;
                    }
                    if (reason instanceof String name) {
                        if (name.equals("killed")) {
                            if (!(entity instanceof LivingEntity livingEntity) || livingEntity.isDeadOrDying()) {
                                real = Entity.RemovalReason.KILLED;
                            }
                        }
                        if (name.equals("discarded")) {
                            real = Entity.RemovalReason.DISCARDED;
                        }
                    }
                    if (((EntityAccessor) entity).zenith$getRemovalReason() == null && real != null) {
                        IZenithEntityExtension.of(entity).zenith$remove();
                        ((EntityAccessor) entity).zenith$setRemovalReason(real);
                        if (real.shouldDestroy()) {
                            entity.stopRiding();
                        }
                        entity.getPassengers().forEach(Entity::stopRiding);
                        ((EntityAccessor) entity).zenith$getLevelCallback().onRemove(real);
                    }
                }
            }
        } catch (Throwable t) {
            TheZenithMod.LOGGER.error("Found an Throwable when removing an entity", t);
        }
    }

    public static boolean modifyIsRemoved(boolean origin, Object mustEntity) {
        if (mustEntity instanceof Entity entity) {
            if (entity.level().isClientSide()) {
                return origin;
            }
            if (entity instanceof ZenithProjectile) {
                return origin;
            }
            if (IZenithEntityExtension.of(entity).zenith$removed()) {
                if (((EntityAccessor) entity).zenith$getRemovalReason() == null) {
                    ((EntityAccessor) entity).zenith$setRemovalReason(Entity.RemovalReason.KILLED);
                }
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
