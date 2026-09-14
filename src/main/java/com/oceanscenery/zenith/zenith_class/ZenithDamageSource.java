package com.oceanscenery.zenith.zenith_class;

import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class ZenithDamageSource extends DamageSource {
    private float intended_damage=0f;

    public static ZenithDamageSource zenith(Entity attacker){
        return new ZenithDamageSource(
                attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ZenithDamageTypes.ZENITH),
                attacker
        );
    }

    public static ZenithDamageSource zenith_knock(Entity attacker){
        return new ZenithDamageSource(
                attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ZenithDamageTypes.ZENITH_KNOCKBACK),
                attacker
        );
    }

    public ZenithDamageSource(Holder<DamageType> type, @Nullable Entity entity) {
        super(type, entity);
    }

    public ZenithDamageSource setDamage(float amount){
        this.intended_damage=amount;
        return this;
    }

    public float getIntendedDamage(){
        return this.intended_damage;
    }
}
