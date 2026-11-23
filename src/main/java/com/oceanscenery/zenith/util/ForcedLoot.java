package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.mixin.accessor.LivingEntityAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.invoke.MethodHandle;

public final class ForcedLoot {
    private static final ThreadLocal<ForcedLoot> CURRENT = new ThreadLocal<>();
    private final LivingEntity victim;
    private boolean enteredBase;

    private ForcedLoot(LivingEntity victim) {
        this.victim = victim;
    }

    public static boolean active(Object entity) {
        ForcedLoot current = CURRENT.get();
        return current != null && current.victim == entity;
    }

    public static int operand(int original, Object entity, int forced) {
        return active(entity) ? forced : original;
    }

    public static void enteringBase(LivingEntity entity) {
        ForcedLoot current = CURRENT.get();
        if (current != null && current.victim == entity) {
            current.enteredBase = true;
        }
    }

    public static void drop(LivingEntity entity, DamageSource source) {
        ForcedLoot previous = CURRENT.get();
        ForcedLoot current = new ForcedLoot(entity);
        CURRENT.set(current);
        try {
            ((LivingEntityAccessor) entity).callDropAllDeathLoot(source);
            if (!current.enteredBase && BaseMethod.HANDLE != null) {
                CapabilitiesHandler.setHealth(entity, -1f);
                CapabilitiesHandler.setHitByZenith(entity, b -> true, Long.MAX_VALUE - 1 - entity.level().getGameTime());
                BaseMethod.HANDLE.invokeExact(entity, source);
            }
        } catch (Throwable failure) {
            if (failure instanceof Error error) {
                throw error;
            }
            TheZenithMod.LOGGER.error("Failed to finish forced loot for {}", entity.getUUID(), failure);
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    private static class BaseMethod {
        static final MethodHandle HANDLE = resolve();

        private static MethodHandle resolve() {
            try {
                var method = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "m_6668_", DamageSource.class);
                return JavaUtil.IMPL.get().get().unreflectSpecial(method, LivingEntity.class);
            } catch (ReflectiveOperationException | RuntimeException e) {
                TheZenithMod.LOGGER.error("Cannot resolve LivingEntity.dropAllDeathLoot", e);
                return null;
            }
        }
    }
}
