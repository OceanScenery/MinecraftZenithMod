package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.registry.*;
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
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Stack;

@EventBusSubscriber(modid=TheZenithMod.MOD_ID)
public class DamageHandle {
    private static boolean caughtException=false;

    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true)
    public static void applyPercentage(LivingIncomingDamageEvent event){
        if(event.getSource().is(ZenithDamageType.ZENITH) || event.getSource().is(ZenithDamageType.ZENITH_KNOCKBACK)){
            event.setCanceled(false);
            LivingEntity living = event.getEntity();
            float maxHealth=living.getMaxHealth();
            float pastDamage=living.getData(ZenithAttachment.ZENITH_INITIAL_DAMAGE_MARK);
            pastDamage+=maxHealth*0.01f;
            living.setData(ZenithAttachment.ZENITH_INITIAL_DAMAGE_MARK,pastDamage);
            event.setAmount(pastDamage);
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
    public static void onHurtEve(LivingDamageEvent.Pre event){
        if(!event.getEntity().level().isClientSide()){
            LivingEntity victim = event.getEntity();
            event.getOriginalDamage();
            DamageSource source = event.getSource();
            if (source.is(ZenithDamageType.ZENITH) || source.is(ZenithDamageType.ZENITH_KNOCKBACK)) {
                float initial_damage=victim.getData(ZenithAttachment.ZENITH_INITIAL_DAMAGE_MARK);
                float damage = event.getNewDamage();
                float non_p=ZenithConfigs.getEnsuredDamageForNonPlayer()*initial_damage;
                float for_p=ZenithConfigs.getEnsuredDamageForPlayer()*initial_damage;
                if(victim instanceof Player){
                    if (damage<for_p) {
                        event.setNewDamage(for_p);
                    }
                }else{
                    if (damage<non_p) {
                        event.setNewDamage(non_p);
                    }
                }
                victim.setData(ZenithAttachment.ZENITH_INITIAL_DAMAGE_MARK,0f);
            }
        }
    }

    public static boolean applyDamage(LivingEntity attacker,Entity victim,@NotNull ItemStack weapon){
        return applyDamage(attacker,victim,weapon,1);
    }

    public static boolean applyDamage(LivingEntity attacker, Entity victim, @NotNull ItemStack weapon, float multiply){
        if(attacker.level() instanceof ServerLevel serverLevel){
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
            damage=damage*(float)(Math.min(ZenithConfigs.getRangedFactor()*((TheZenithMod.TERRA_LOADED&&ZenithConfigs.ZENITH_CONFIG.enable_terra_damage_modifier.get())?10:1),1))*multiply;
            victim.setData(ZenithAttachment.ZENITH_INITIAL_DAMAGE_MARK,damage);
            victim.invulnerableTime=0;

            if(ZenithConfigs.ZENITH_CONFIG.enable_bypass_invulnerable.get()){
                damageProcess(victim,attacker,damage,source);
            }

            if(!victim.hurtServer(serverLevel,source,damage) && victim instanceof LivingEntity living && weapon.get(ZenithDataComponents.ATTACK_MODE).mode().equals(AttackMode.Mode.ALL)){
                try{
                    MethodHandles.Lookup handles=MethodHandles.privateLookupIn(LivingEntity.class,MethodHandles.lookup());
                    MethodHandle handle=handles.findSpecial(LivingEntity.class,"hurtServer",MethodType.methodType(boolean.class,ServerLevel.class,DamageSource.class,float.class), LivingEntity.class);
                    if(!(boolean)handle.invoke(living,serverLevel,source,damage)){
                        damage+=living.getMaxHealth()*0.01f;
                        victim.setData(ZenithAttachment.ZENITH_INITIAL_DAMAGE_MARK,damage);
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
                damage+=living.getMaxHealth()*0.01f;
                @SuppressWarnings("unchecked")
                EntityDataAccessor<Float> health=(EntityDataAccessor<Float>)method.invoke();
                float hp=living.getEntityData().get(health);
                living.getEntityData().set(health,hp-damage);
                if(attacker instanceof Player player){
                    living.setLastHurtByPlayer(player,20);
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
