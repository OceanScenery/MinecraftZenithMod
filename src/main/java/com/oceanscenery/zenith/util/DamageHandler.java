package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mixin.accessor.EntityAccessor;
import com.oceanscenery.zenith.mixin.accessor.LivingEntityAccessor;
import com.oceanscenery.zenith.mixin.accessor.PlayerAccessor;
import com.oceanscenery.zenith.mixin.accessor.ServerPlayerAccessor;
import com.oceanscenery.zenith.mod_class.ZenithDamageSource;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.mod_class.entity.ZenithProjectile;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithDataComponents;
import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class DamageHandler {
    private static final ThreadLocal<Boolean> inMethod = ThreadLocal.withInitial(() -> false);

    public static final AttackFilter DEFAULT_FILTER = (attacker, victim, weapon) -> {
        if (weapon == null) {
            return false;
        }
        if (weapon.is(ZenithItems.ZENITH.get())) {
            if (victim instanceof ZenithProjectile) {
                return false;
            }
            if ((victim instanceof ItemEntity || victim instanceof ExperienceOrb) && !ZenithConfigs.ZENITH_CONFIG.enable_attack_item.get()) {
                return false;
            }
            AttackMode atk = Optional.ofNullable(weapon.get(ZenithDataComponents.ATTACK_MODE)).orElse(AttackMode.LIVING_ENTITY);
            if (!victim.isAttackable() && atk != AttackMode.ALL) {
                return false;
            }
            if (!(victim instanceof LivingEntity) && atk == AttackMode.LIVING_ENTITY) {
                return false;
            }
            if (victim instanceof ArmorStand && atk != AttackMode.ALL) {
                return false;
            }
            if (!AttachmentUtil.checkCanAttack(attacker, victim)) {
                return false;
            }
            return true;
        }
        return false;
    };

    public interface AttackFilter {
        boolean canAttack(Entity attacker, Entity victim, ItemStack weapon);
    }

    public static class VictimRecord {
        public VictimRecord(float health, float damage, float extra) {
            expectedDamage = damage;
            extraDamage = extra;
            initialHealth = health;
        }

        public float extraDamage = 0f;
        public float expectedDamage = 0f;
        public float initialHealth;
    }

    public static void addLivingVictim(LivingEntity livingVictim, float currDamage) {
        addLivingVictim(livingVictim, currDamage, 0f);
    }

    public static void addLivingVictim(LivingEntity livingVictim, float currDamage, float extraDamage) {
        if (StaticCollections.checkedVictims.containsKey(livingVictim)) {
            VictimRecord rec = StaticCollections.checkedVictims.get(livingVictim);
            rec.expectedDamage += currDamage;
            rec.extraDamage += extraDamage;

        } else {
            StaticCollections.checkedVictims.put(livingVictim, new VictimRecord(livingVictim.getHealth(), currDamage, extraDamage));
        }
    }

    private static boolean applyDamageAmount(Entity victim, ZenithDamageSource source, float damage, ItemStack weapon, float scale) {
        if (inMethod.get()) {
            return false;
        }
        inMethod.set(true);
        try {
            Entity attacker = source.getEntity();

            if (attacker == null || !(attacker.level() instanceof ServerLevel)) {
                return false;
            }
            if (attacker instanceof LivingEntity livingAttacker) {
                Optional.of(livingAttacker.level()).filter(l -> l instanceof ServerLevel).ifPresent(
                    level -> {
                        EnchantmentHelper.doPostAttackEffectsWithItemSource(
                            (ServerLevel) level, victim, source, weapon
                        );
                    }
                );
            }
            if (victim instanceof LivingEntity living) {
                if (living.level() instanceof ServerLevel serverLevel) {
                    damage = EnchantmentHelper.modifyDamage(
                        serverLevel, weapon, living, source, damage
                    );
                }
            }
            damage = damage * (float) (Math.min(ConfigUtil.rangedFactor() * ((TheZenithMod.TERRA_LOADED && ZenithConfigs.ZENITH_CONFIG.enable_terra_damage_modifier.get()) ? 10 : 1), 1)) * scale;
            victim.invulnerableTime = 0;

            StaticCollections.checkedVictims.clear();

            if (victim instanceof LivingEntity living) {
                if (!AttachmentUtil.getHealth(living).valid(living)) {
                    float value = living.getHealth();
                    if (Float.isNaN(value) || Float.isInfinite(value)) {
                        value = -1f;
                    }
                    AttachmentUtil.setHealth(living, value);
                }
            }

            AttachmentUtil.setHitByZenith(victim, origin -> true);
            victim.hurt(source.setDamage(damage), damage);

            if (victim instanceof LivingEntity livingVictim && !StaticCollections.checkedVictims.containsKey(livingVictim)) {
                try {
                    addLivingVictim(
                        livingVictim, damage + ConfigUtil.healthPercentage() * livingVictim.getMaxHealth(),
                        Optional.ofNullable(AttachmentUtil.getMark(livingVictim).getMap().remove(attacker.getUUID())).orElse(0f)
                    );
                } catch (Exception e) {
                    TheZenithMod.LOGGER.error("Capability registered error", e);
                }
            }

            for (Map.Entry<LivingEntity, VictimRecord> current : new ArrayList<>(StaticCollections.checkedVictims.entrySet())) {
                float factor;
                if (ConfigUtil.bypass()) {
                    factor = 1f;
                } else {
                    factor = (float) ConfigUtil.ensuredDamage((current.getKey() instanceof Player));
                }

                LivingEntity currVictim = current.getKey();
                VictimRecord currRecord = current.getValue();
                float realDamage = currRecord.initialHealth - currVictim.getHealth();
                float expectedDamage = currRecord.expectedDamage * factor + currRecord.extraDamage;

                if (realDamage < expectedDamage) {
                    setHealthAndHurt(currVictim, currRecord.initialHealth - expectedDamage, source);
                }
            }

            StaticCollections.checkedVictims.clear();

            victim.invulnerableTime = 0;
            return true;
        } finally {
            inMethod.set(false);
        }
    }

    public static boolean applyDamage(ZenithProjectile projectile, Entity victim, @NotNull ItemStack weapon, float scale, AttackFilter filter) {
        if (!projectile.level().isClientSide()) {
            float damage = projectile.getDamage();
            Entity attacker = projectile.getOwner();
            ZenithDamageSource source;

            if (!filter.canAttack(attacker, victim, weapon)) {
                return false;
            }

            source = ZenithDamageSource.getRightSource(
                Optional.ofNullable(attacker).orElse(projectile),
                filter, weapon
            );
            return applyDamageAmount(victim, source, damage, weapon, scale);
        }
        return false;
    }

    public static boolean applyDamage(ZenithProjectile projectile, Entity victim, @NotNull ItemStack weapon, float scale) {
        return applyDamage(projectile, victim, weapon, scale, DEFAULT_FILTER);
    }

    public static boolean applyDirectDamage(@NotNull Entity attacker, Entity victim, ItemStack weapon, float scale, float damage, AttackFilter filter) {
        if (!attacker.level().isClientSide()) {
            ZenithDamageSource source;

            if (!filter.canAttack(attacker, victim, weapon)) {
                return false;
            }

            source = ZenithDamageSource.getRightSource(attacker, filter, weapon);
            return applyDamageAmount(victim, source, damage, weapon, scale);
        }
        return false;
    }

    public static boolean applyDirectDamage(@NotNull Entity attacker, Entity victim, ItemStack weapon, float scale, float damage) {
        return applyDirectDamage(attacker, victim, weapon, scale, damage, DEFAULT_FILTER);
    }

    public static void setHealthAndHurt(LivingEntity livingVictim, float health, DamageSource source) {
        Entity attacker = source.getEntity();
        float prevCheckedHealth = livingVictim.getHealth();
        if (attacker instanceof Player player) {
            livingVictim.setLastHurtByPlayer(player);
        } else if (attacker instanceof LivingEntity livingAttacker) {
            livingVictim.setLastHurtByMob(livingAttacker);
        }
        float damage = livingVictim.getHealth() - health;
        livingVictim.getCombatTracker().recordDamage(source, damage);
        attacker.level().broadcastDamageEvent(livingVictim, source);

        SynchedEntityData data = ((EntityAccessor) livingVictim).getRealEntityData();
        EntityDataAccessor<Float> healthId = LivingEntityAccessor.getHealthId();

        boolean shouldCheck = false;
        if (data.get(healthId) > 0f || livingVictim.getHealth() > 0f) {
            shouldCheck = true;
        }

        livingVictim.setHealth(health);
        AttachmentUtil.setHealth(livingVictim, health);
        float postCheckedHealth = livingVictim.getHealth();

        ConfigUtil.redirectDamageAction(livingVictim, prevCheckedHealth, postCheckedHealth, damage, source);

        if (shouldCheck && livingVictim.level() instanceof ServerLevel serverLevel) {
            checkDeath(livingVictim, serverLevel, source);
        }
    }

    public static void decideDeath(LivingEntity livingVictim, float prevCheckedHealth, float postCheckedHealth, float damage, DamageSource source) {
        if (postCheckedHealth >= prevCheckedHealth && !livingVictim.isDeadOrDying()) {
            deathProcess(livingVictim, source);
        } else if (livingVictim.isDeadOrDying() || livingVictim.getHealth() <= 0f) {
            deathProcess(livingVictim, source);
        }
    }

    public static void deathProcess(LivingEntity livingVictim, DamageSource source) {
        if (livingVictim.level() instanceof ServerLevel serverLevel) {
            if (!(livingVictim instanceof ServerPlayer sp) || !sp.isSpectator()) {
                ((LivingEntityAccessor) livingVictim).callDropAllDeathLoot(serverLevel, source);
            }
        }

        if (!(livingVictim instanceof ServerPlayer)) {
            LivingEntity killer = livingVictim.getKillCredit();
            if (killer != null) {
                killer.awardKillScore(livingVictim, ((LivingEntityAccessor) livingVictim).zenith$getDeathScore(), source);
            }
            if (livingVictim.isSleeping()) {
                livingVictim.stopSleeping();
            }
            livingVictim.stopUsingItem();
            ((LivingEntityAccessor) livingVictim).setDead(true);
            if (livingVictim.level() instanceof ServerLevel serverLevel) {
                if (source.getEntity() == null || source.getEntity().killedEntity(serverLevel, livingVictim)) {
                    livingVictim.gameEvent(GameEvent.ENTITY_DIE);
                }
                serverLevel.broadcastEntityEvent(livingVictim, (byte) 3);
            }
        }

        if (livingVictim instanceof ServerPlayer serverPlayer) {
            serverPlayer.gameEvent(GameEvent.ENTITY_DIE);

            ((PlayerAccessor) serverPlayer).zenith$removeEntitiesOnShoulder();
            if (serverPlayer.level().getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
                ((ServerPlayerAccessor) serverPlayer).zenith$tellNeutralMobsThatIDied();
            }

            serverPlayer.getScoreboard().forAllObjectives(
                ObjectiveCriteria.DEATH_COUNT, serverPlayer, ScoreAccess::increment
            );
            LivingEntity livingentity = serverPlayer.getKillCredit();
            if (livingentity != null) {
                serverPlayer.awardStat(Stats.ENTITY_KILLED_BY.get(livingentity.getType()));
                livingentity.awardKillScore(
                    serverPlayer, ((LivingEntityAccessor) serverPlayer).zenith$getDeathScore(), source
                );
            }

            serverPlayer.level().broadcastEntityEvent(serverPlayer, (byte) 3);
            serverPlayer.awardStat(Stats.DEATHS);
            serverPlayer.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
            serverPlayer.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            serverPlayer.clearFire();
            serverPlayer.setTicksFrozen(0);
            serverPlayer.setSharedFlagOnFire(false);
            serverPlayer.getCombatTracker().recheckStatus();
            serverPlayer.setLastDeathLocation(
                Optional.of(GlobalPos.of(serverPlayer.level().dimension(), serverPlayer.blockPosition()))
            );
        }

        EntityUtil.remove(livingVictim);
    }

    public static void checkDeath(LivingEntity livingEntity, ServerLevel serverLevel, DamageSource source) {
        SynchedEntityData data = ((EntityAccessor) livingEntity).getRealEntityData();
        EntityDataAccessor<Float> healthId = LivingEntityAccessor.getHealthId();

        if ((data.get(healthId) <= 0f || livingEntity.getHealth() <= 0f) && !livingEntity.isRemoved()) {
            if (!((LivingEntityAccessor) livingEntity).callCheckTotemDeathProtection(source)) {
                livingEntity.die(source);
                ((LivingEntityAccessor) livingEntity).setDead(true);
            } else {
                AttachmentUtil.setHealth(livingEntity, Math.min(20f, livingEntity.getMaxHealth()));
            }
        }
    }
}
