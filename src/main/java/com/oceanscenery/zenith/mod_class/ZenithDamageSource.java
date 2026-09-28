package com.oceanscenery.zenith.mod_class;

import com.oceanscenery.zenith.registry.ZenithDamageType;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class ZenithDamageSource extends DamageSource {
    private static final Holder<DamageType> ZENITH = null;
    private static final Holder<DamageType> ZENITH_KNOCKBACK = null;
    private float intended_damage = 0f;

    public ZenithDamageSource(Holder<DamageType> type, @Nullable Entity entity) {
        super(type, entity);
    }

    public static ZenithDamageSource zenith(Entity attacker) {
        return new ZenithDamageSource(
            attacker.level().registryAccess().holderOrThrow(ZenithDamageType.ZENITH), attacker
        );
    }

    public static ZenithDamageSource zenith_knock(Entity attacker) {
        return new ZenithDamageSource(
            attacker.level().registryAccess().holderOrThrow(ZenithDamageType.ZENITH_KNOCKBACK), attacker
        );
    }

    public ZenithDamageSource setDamage(float amount) {
        this.intended_damage = amount;
        return this;
    }

    public float getIntendedDamage() {
        return this.intended_damage;
    }
}
