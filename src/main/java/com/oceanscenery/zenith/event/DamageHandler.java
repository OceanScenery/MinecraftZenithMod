package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DamageHandler {
    private static boolean caughtException=false;
    private static Holder<DamageType> ZENITH;
    private static Holder<DamageType> ZENITH_KNOCKBACK;
    private static boolean initialized=false;

    public static void initialize(Level level){
        RegistryAccess access=level.registryAccess();
        Registry<DamageType> registry=access.registryOrThrow(Registries.DAMAGE_TYPE);
        ZENITH=registry.getHolderOrThrow(ZenithDamageTypes.ZENITH);
        ZENITH_KNOCKBACK=registry.getHolderOrThrow(ZenithDamageTypes.ZENITH_KNOCKBACK);
        initialized=true;
    }

    public static DamageSource getSource(LivingEntity attacker){
        if(!initialized){
            initialize(attacker.level());
        }
        return ZenithConfigs.ZENITH_CONFIG.disable_knockback.get()?new DamageSource(ZENITH,attacker):new DamageSource(ZENITH_KNOCKBACK,attacker);
    }

    @SubscribeEvent(priority= EventPriority.HIGHEST,receiveCanceled=true)
    public static void applyPercentage(LivingHurtEvent event){
        if(event.getSource().is(ZenithDamageTypes.ZENITH) || event.getSource().is(ZenithDamageTypes.ZENITH_KNOCKBACK)){
            event.setCanceled(false);
            LivingEntity living = event.getEntity();
            float maxHealth=living.getMaxHealth();
            float pastDamage=CapabilitiesHandler.getMark(living);
            pastDamage+=maxHealth*0.01f;
            CapabilitiesHandler.setMark(living,pastDamage+CapabilitiesHandler.getMark(living));
            event.setAmount(pastDamage);
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
    public static void onHurtEve(LivingDamageEvent event){
        if(!event.getEntity().level().isClientSide){
            LivingEntity victim = event.getEntity();
            DamageSource source = event.getSource();
            if (source.is(ZenithDamageTypes.ZENITH) || source.is(ZenithDamageTypes.ZENITH_KNOCKBACK)) {
                float initial_damage=CapabilitiesHandler.getMark(victim);
                float damage=event.getAmount();
                float non_p= ZenithConfigs.getEnsuredDamageForNonPlayer()*initial_damage;
                float for_p=ZenithConfigs.getEnsuredDamageForPlayer()*initial_damage;
                if(victim instanceof Player){
                    if (damage<for_p) {
                        event.setAmount(for_p);
                    }
                }else{
                    if (damage<non_p) {
                        event.setAmount(non_p);
                    }
                }
                CapabilitiesHandler.setMark(victim,0f);
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
            source=getSource(attacker);
            EnchantmentHelper.doPostHurtEffects(attacker,victim);
            EnchantmentHelper.doPostDamageEffects(attacker,victim);
            if(victim instanceof LivingEntity living){
                damage+=EnchantmentHelper.getDamageBonus(weapon,living.getMobType());
            }
            damage=damage*(float)(Math.min(ZenithConfigs.getRangedFactor()*((TheZenithMod.TERRA_LOADED&&ZenithConfigs.ZENITH_CONFIG.enable_terra_damage_modifier.get())?10:1),1))*multiply;
            CapabilitiesHandler.setMark(victim,damage);
            victim.invulnerableTime=0;

            if(ZenithConfigs.ZENITH_CONFIG.enable_bypass_invulnerable.get()){
                damageProcess(victim,attacker,damage,source);
            }

            if(!victim.hurt(source,damage) && victim instanceof LivingEntity living && WeaponHandler.getAttackMode(weapon)==2){
                try{
                    MethodHandles.Lookup handles=MethodHandles.privateLookupIn(LivingEntity.class,MethodHandles.lookup());
                    MethodHandle handle=handles.findSpecial(LivingEntity.class,"hurt",MethodType.methodType(boolean.class,DamageSource.class,float.class), LivingEntity.class);
                    if(!(boolean)handle.invoke(living,source,damage)){
                        damage+=living.getMaxHealth()*0.01f;
                        CapabilitiesHandler.setMark(victim,damage);
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
        if(stack.is(ZenithItems.ZENITH.get())){
            if(victim instanceof LivingEntity living && living.isDeadOrDying()){
                return false;
            }
            if((victim instanceof ItemEntity || victim instanceof ExperienceOrb) && !ZenithConfigs.ZENITH_CONFIG.enable_attack_item.get()){
                return false;
            }
            int atk=WeaponHandler.getAttackMode(stack);
            boolean attackPlayer=WeaponHandler.attackPlayer(stack);
            if (victim instanceof Player && !attackPlayer) {
                return false;
            }
            if (!victim.isAttackable() && atk!=2) {
                return false;
            }
            if (!(victim instanceof LivingEntity) && !(atk>0)) {
                return false;
            }
            if (victim instanceof ArmorStand && atk!=2) {
                return false;
            }
            return true;
        }
        return false;
    }
}
