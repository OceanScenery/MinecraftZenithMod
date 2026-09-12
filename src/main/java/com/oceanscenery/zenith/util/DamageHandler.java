package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mixin.EntityAccessor;
import com.oceanscenery.zenith.mixin.LivingEntityAccessor;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.mod_class.entity.ZenithProjectile;
import com.oceanscenery.zenith.registry.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DamageHandler {
    public static class VictimRecord{
        public VictimRecord(float health,float damage){
            expectedDamage=damage;
            initialHealth=health;
        }

        public float expectedDamage=0f;
        public float initialHealth;
    }

    public static final Map<LivingEntity,VictimRecord> checkedVictims=new HashMap<>();

    public static void addLivingVictim(LivingEntity livingVictim,float currDamage){
        if(checkedVictims.containsKey(livingVictim)){
            checkedVictims.get(livingVictim).expectedDamage+=currDamage;
        }else{
            checkedVictims.put(livingVictim,new VictimRecord(getTrueHealth(livingVictim),currDamage));
        }
    }

    public static float getTrueHealth(LivingEntity livingVictim){
        SynchedEntityData data=((EntityAccessor)livingVictim).getRealEntityData();
        return data.get(LivingEntityAccessor.getHealthId());
    }

    public static boolean applyDamage(ZenithProjectile projectile,Entity victim,@NotNull ItemStack weapon){
        return applyDamage(projectile,victim,weapon,1);
    }

    private static boolean applyDamageAmount(Entity victim,DamageSource source,float damage,ItemStack weapon,float scale){
        Entity attacker=source.getEntity();

        if(attacker==null || !(attacker.level() instanceof ServerLevel serverLevel)){
            return false;
        }

        EnchantmentHelper.doPostAttackEffectsWithItemSource((ServerLevel)attacker.level(), victim, source,weapon);
        damage=EnchantmentHelper.modifyDamage((ServerLevel)attacker.level(),weapon, victim, source, damage);
        damage=damage*(float)(Math.min(ZenithConfigs.getRangedFactor()*((TheZenithMod.TERRA_LOADED&&ZenithConfigs.ZENITH_CONFIG.enable_terra_damage_modifier.get())?10:1),1))*scale;
        victim.invulnerableTime=0;

        checkedVictims.clear();

        victim.hurtServer(serverLevel,source,damage);
        if(victim instanceof LivingEntity livingVictim && !checkedVictims.containsKey(livingVictim)){
            addLivingVictim(livingVictim,damage+ConfigUtil.healthPercentage()*livingVictim.getMaxHealth());
        }

        for (Map.Entry<LivingEntity,VictimRecord> current:new ArrayList<>(checkedVictims.entrySet())) {
            float factor;
            if(ZenithConfigs.ZENITH_CONFIG.enable_bypass_invulnerable.get()){
                factor=1f;
            }else{
                factor=(current.getKey() instanceof Player)?ZenithConfigs.getEnsuredDamageForPlayer():ZenithConfigs.getEnsuredDamageForNonPlayer();
            }

            LivingEntity currVictim=current.getKey();
            VictimRecord currRecord=current.getValue();
            float realDamage=currRecord.initialHealth-getTrueHealth(currVictim);
            float expectedDamage=currRecord.expectedDamage*factor;

            if(realDamage<expectedDamage){
                setHealthAndHurt(currVictim,currRecord.initialHealth-expectedDamage,source);
            }
        }

        checkedVictims.clear();

        victim.invulnerableTime=0;
        return true;
    }

    public static boolean applyDamage(ZenithProjectile projectile, Entity victim, @NotNull ItemStack weapon, float scale){
        if(!projectile.level().isClientSide()){
            float damage=projectile.getDamage();
            Entity attacker=projectile.getOwner();
            DamageSource source;

            if(!canAttack(attacker,victim,weapon)){
                return false;
            }

            if(attacker==null){
                source=projectile.damageSources().source(ZenithDamageType.ZENITH,projectile);
            }else{
                if(ZenithConfigs.ZENITH_CONFIG.disable_knockback.get()){
                    source=attacker.damageSources().source(ZenithDamageType.ZENITH,attacker);
                }else{
                    source=attacker.damageSources().source(ZenithDamageType.ZENITH_KNOCKBACK,attacker);
                }
            }
            return applyDamageAmount(victim,source,damage,weapon,scale);
        }
        return false;
    }

    public static boolean applyDirectDamage(@NotNull Entity attacker, Entity victim, ItemStack weapon, float scale, float damageCount){
        if(!attacker.level().isClientSide()){
            float damage=damageCount;
            DamageSource source;

            if(!canAttack(attacker,victim,weapon)){
                return false;
            }

            if (ZenithConfigs.ZENITH_CONFIG.disable_knockback.get()) {
                source=attacker.damageSources().source(ZenithDamageType.ZENITH, attacker);
            } else {
                source=attacker.damageSources().source(ZenithDamageType.ZENITH_KNOCKBACK, attacker);
            }
            return applyDamageAmount(victim,source,damage,weapon,scale);
        }
        return false;
    }

    public static void setHealthAndHurt(LivingEntity livingVictim,float health,DamageSource source){
        Entity attacker=source.getEntity();
        if (attacker instanceof Player player) {
            livingVictim.setLastHurtByPlayer(player,100);
        } else if(attacker instanceof LivingEntity livingAttacker){
            livingVictim.setLastHurtByMob(livingAttacker);
        }
        float damage=getTrueHealth(livingVictim)-health;
        livingVictim.getCombatTracker().recordDamage(source, damage);
        attacker.level().broadcastDamageEvent(livingVictim, source);

        SynchedEntityData data=((EntityAccessor)livingVictim).getRealEntityData();
        EntityDataAccessor<Float> healthId=LivingEntityAccessor.getHealthId();

        boolean shouldCheck=false;
        if(data.get(healthId)>0f){
            shouldCheck=true;
        }

        data.set(healthId,health);

        if(shouldCheck){
            checkDeath(livingVictim, source);
        }
    }

    public static boolean canAttack(Entity attacker,Entity victim,ItemStack stack){
        if(stack==null){
            return false;
        }
        if(stack.is(ZenithItems.ZENITH)){
            if(victim instanceof LivingEntity living && living.isDeadOrDying()){
                return false;
            }
            if((victim instanceof ItemEntity || victim instanceof ExperienceOrb) && !ZenithConfigs.ZENITH_CONFIG.enable_attack_item.get()){
                return false;
            }
            if(stack.get(ZenithDataComponents.ATTACK_MODE)==null){
                stack.set(ZenithDataComponents.ATTACK_MODE,new AttackMode(AttackMode.Mode.LIVING_ENTITY,true));
            }
            AttackMode atk=stack.get(ZenithDataComponents.ATTACK_MODE);
            if (atk != null && victim instanceof Player && !atk.attackPlayer()) {
                return false;
            }
            if (atk != null && !victim.isAttackable() && !atk.getMode().equals(AttackMode.Mode.ALL)) {
                return false;
            }
            if (atk != null && !(victim instanceof LivingEntity) && !(atk.getMode().equals(AttackMode.Mode.ATTACKABLE_ENTITY) || atk.getMode().equals(AttackMode.Mode.ALL))) {
                return false;
            }
            if (atk != null && victim instanceof ArmorStand && !atk.getMode().equals(AttackMode.Mode.ALL)) {
                return false;
            }
            if(!AttachmentUtil.checkCanAttack(attacker,victim)){
                return false;
            }
            return true;
        }
        return false;
    }

    public static void checkDeath(LivingEntity livingEntity,DamageSource source){
        SynchedEntityData data=((EntityAccessor)livingEntity).getRealEntityData();
        EntityDataAccessor<Float> healthId=LivingEntityAccessor.getHealthId();

        if(data.get(healthId)<=0f){
            if(!((LivingEntityAccessor)livingEntity).callCheckTotemDeathProtection(source)){
                livingEntity.die(source);
            }
        }
    }
}
