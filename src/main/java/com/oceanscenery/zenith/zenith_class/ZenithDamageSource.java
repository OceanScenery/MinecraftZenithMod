package com.oceanscenery.zenith.zenith_class;

import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import com.oceanscenery.zenith.util.ConfigUtil;
import com.oceanscenery.zenith.util.DamageHandler;
import com.oceanscenery.zenith.util.DamageHandler.AttackFilter;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ZenithDamageSource extends DamageSource {
    private float intended_damage = 0f;
    private final Set<LivingEntity> attached = new HashSet<>();
    private final AttackFilter filterData;
    private final ItemStack weapon;

    private ZenithDamageSource(Holder<DamageType> type, @Nullable Entity entity, @NotNull ItemStack weapon) {
        super(type, entity);
        this.filterData = DamageHandler.DEFAULT_FILTER;
        this.weapon = weapon;
    }

    private ZenithDamageSource(Holder<DamageType> type, @Nullable Entity entity, @NotNull ItemStack weapon, @NotNull AttackFilter filter) {
        super(type, entity);
        this.filterData = filter;
        this.weapon = weapon;
    }

    private static ZenithDamageSource zenith(Entity attacker, AttackFilter filter, ItemStack weapon) {
        if (attacker == null) {
            throw new NullPointerException("Attacker of ZenithDamageSource should be non-null");
        }
        return new ZenithDamageSource(
            attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ZenithDamageTypes.ZENITH),
            attacker,
            weapon,
            filter
        );
    }

    private static ZenithDamageSource zenithKnock(Entity attacker, AttackFilter filter, ItemStack weapon) {
        if (attacker == null) {
            throw new NullPointerException("Attacker of ZenithDamageSource should be non-null");
        }
        return new ZenithDamageSource(
            attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ZenithDamageTypes.ZENITH_KNOCKBACK),
            attacker,
            weapon,
            filter
        );
    }

    public ZenithDamageSource setDamage(float initial) {
        this.intended_damage = initial;
        return this;
    }

    public static ZenithDamageSource getRightSource(Entity attacker, AttackFilter filter, ItemStack weapon) {
        return ConfigUtil.shouldKnock() ? zenithKnock(attacker, filter, weapon) : zenith(attacker, filter, weapon);
    }

    public static ZenithDamageSource getRightSource(Entity attacker, ItemStack weapon) {
        return getRightSource(attacker, DamageHandler.DEFAULT_FILTER, weapon);
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

    public AttackFilter getFilter() {
        return this.filterData;
    }

    public boolean canAttack(Entity victim){
        return this.getFilter().canAttack(this.getEntity(),victim,this.weapon);
    }
}
