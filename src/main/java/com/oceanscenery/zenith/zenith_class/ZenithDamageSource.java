package com.oceanscenery.zenith.zenith_class;

import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import com.oceanscenery.zenith.util.ConfigUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ZenithDamageSource extends DamageSource {
    private float intended_damage = 0f;

    private Set<LivingEntity> attached = new HashSet<>();

    private ZenithDamageSource(Holder<DamageType> type, @Nullable Entity entity) {
        super(type, entity);
    }

    public static ZenithDamageSource zenith(Entity attacker) {
        if (attacker == null) {
            throw new NullPointerException("Attacker of ZenithDamageSource should be non-null");
        }
        return new ZenithDamageSource(
            attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ZenithDamageTypes.ZENITH), attacker
        );
    }

    public static ZenithDamageSource zenithKnock(Entity attacker) {
        if (attacker == null) {
            throw new NullPointerException("Attacker of ZenithDamageSource should be non-null");
        }
        return new ZenithDamageSource(
            attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ZenithDamageTypes.ZENITH_KNOCKBACK), attacker
        );
    }

    public ZenithDamageSource setDamage(float initial) {
        this.intended_damage = initial;
        return this;
    }

    public static ZenithDamageSource getRightSource(Entity attacker) {
        return ConfigUtil.shouldKnock() ? zenithKnock(attacker) : zenith(attacker);
    }

    public float getIntendedDamage() {
        return this.intended_damage;
    }

    public void markEntity(Entity entity) {
        if (entity instanceof LivingEntity living) {
            this.attached.add(living);
        }
    }

    public boolean hasAttached(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return this.attached.contains(living);
        }
        return false;
    }
}
