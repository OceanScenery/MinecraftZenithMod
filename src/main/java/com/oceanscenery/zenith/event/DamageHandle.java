package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithDamageType;
import com.oceanscenery.zenith.registry.ZenithDataComponents;
import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Stack;

@EventBusSubscriber
public class DamageHandle {
    private static boolean caughtException=false;

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onHurtEve(LivingDamageEvent.Pre event){
        if(!event.getEntity().level().isClientSide){
            LivingEntity victim = event.getEntity();
            DamageSource source = event.getSource();
            if (source.is(ZenithDamageType.ZENITH) || source.is(ZenithDamageType.ZENITH_KNOCKBACK)) {
                float damage = event.getNewDamage();
                if(victim instanceof Player){
                    if (damage < ZenithConfigs.getEnsuredDamageForPlayer()) {
                        event.setNewDamage(ZenithConfigs.getEnsuredDamageForPlayer()*(TheZenithMod.TERRA_LOADED?5:1));
                    }
                }else{
                    if (damage < ZenithConfigs.getEnsuredDamageForNonPlayer()*(TheZenithMod.TERRA_LOADED?30:1)) {
                        event.setNewDamage(ZenithConfigs.getEnsuredDamageForNonPlayer()*(TheZenithMod.TERRA_LOADED?30:1));
                    }
                }
            }
        }
    }

    public static boolean applyDamage(LivingEntity attacker,Entity victim,@NotNull ItemStack weapon){
        return applyDamage(attacker,victim,weapon,1);
    }

    public static boolean applyDamage(LivingEntity attacker, Entity victim, @NotNull ItemStack weapon, float multiply){
        if(!attacker.level().isClientSide){
            if(!canAttack(victim,weapon)){
                return false;
            }

            float damage=attacker.getAttribute(Attributes.ATTACK_DAMAGE)==null ? 1:(float)attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
            DamageSource source;
            if(ZenithConfigs.ZENITH_CONFIG.disable_knockback.get()){
                source=attacker.damageSources().source(ZenithDamageType.ZENITH,attacker);
            }else{
                source=attacker.damageSources().source(ZenithDamageType.ZENITH_KNOCKBACK,attacker);
            }
            EnchantmentHelper.doPostAttackEffectsWithItemSource((ServerLevel)attacker.level(), victim, source,weapon);
            damage=EnchantmentHelper.modifyDamage((ServerLevel)attacker.level(),weapon, victim, source, damage);
            damage=damage*(float)(Math.min(ZenithConfigs.getRangedFactor()*(TheZenithMod.TERRA_LOADED?5:1),1))*multiply;
            victim.invulnerableTime=0;
            if(ZenithConfigs.ZENITH_CONFIG.enable_bypass_invulnerable.get()){
                damageProcess(victim,attacker,damage,source);
            }

            if(!victim.hurt(source,damage) && victim instanceof LivingEntity living && weapon.get(ZenithDataComponents.ATTACK_MODE).mode().equals(AttackMode.Mode.ALL)){
                try{
                    MethodHandles.Lookup handles=MethodHandles.privateLookupIn(LivingEntity.class,MethodHandles.lookup());
                    MethodHandle handle=handles.findSpecial(LivingEntity.class,"hurt",MethodType.methodType(boolean.class,DamageSource.class,float.class), LivingEntity.class);
                    if(!(boolean)handle.invoke(living,source,damage)){
                        MethodHandle dmgCtnr=handles.findGetter(LivingEntity.class,"damageContainers", Stack.class);
                        @SuppressWarnings("unchecked")
                        Stack<net.neoforged.neoforge.common.damagesource.DamageContainer> damageContainers=(Stack<net.neoforged.neoforge.common.damagesource.DamageContainer>)dmgCtnr.invoke(living);
                        damageContainers.push(new DamageContainer(source,damage));
                        MethodHandle actualHandle=handles.findSpecial(LivingEntity.class,"actuallyHurt",MethodType.methodType(void.class, DamageSource.class,float.class),LivingEntity.class);
                        actualHandle.invoke(living,source,damage);
                        attacker.level().broadcastDamageEvent(living,source);

                        if(living.isDeadOrDying()){
                            MethodHandle checkTotem=handles.findSpecial(LivingEntity.class,"checkTotemDeathProtection",MethodType.methodType(boolean.class,DamageSource.class), LivingEntity.class);
                            if(!(boolean)checkTotem.invoke(living,source)){
                                living.die(source);
                            }
                        }
                    }
                }catch (Throwable e){
                    if(!caughtException){
                        TheZenithMod.LOGGER.error(e.getMessage());
                        caughtException=true;
                    }
                    victim.invulnerableTime=0;
                    return false;
                }
            }

            victim.invulnerableTime=0;
            return true;
        }
        return false;
    }

    public static boolean damageProcess(Entity victim,LivingEntity attacker,float damage,DamageSource source){
        try{
            MethodHandles.Lookup lookup=MethodHandles.lookup();
            MethodHandles.Lookup pLookup=MethodHandles.privateLookupIn(LivingEntity.class,lookup);
            MethodHandle method=pLookup.findStaticGetter(LivingEntity.class,"DATA_HEALTH_ID",EntityDataAccessor.class);
            if(victim instanceof LivingEntity living){
                @SuppressWarnings("unchecked")
                EntityDataAccessor<Float> health=(EntityDataAccessor<Float>)method.invoke();
                float hp=living.getEntityData().get(health);
                living.getEntityData().set(health,hp-damage);
                if(attacker instanceof Player player){
                    living.setLastHurtByPlayer(player);
                }
                living.getCombatTracker().recordDamage(source,damage);
                attacker.level().broadcastDamageEvent(living,source);
                if(living.getEntityData().get(health)<=0.0){
                    MethodHandle checkTotem=pLookup.findSpecial(LivingEntity.class,"checkTotemDeathProtection",MethodType.methodType(boolean.class,DamageSource.class), LivingEntity.class);
                    MethodHandle die=pLookup.findSpecial(LivingEntity.class,"die",MethodType.methodType(void.class, DamageSource.class), LivingEntity.class);
                    if(!(boolean)checkTotem.invoke(living,source)){
                        die.invoke(living,source);
                    }
                }
            }
        }catch(Throwable e){
            if(!caughtException){
                TheZenithMod.LOGGER.error("unstable! please close the bypass-invulnerability config{}", e.getMessage());
                caughtException = true;
            }
            return false;
        }
        return true;
    }

    public static boolean canAttack(Entity victim,ItemStack stack){
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
            return true;
        }
        return false;
    }
}
