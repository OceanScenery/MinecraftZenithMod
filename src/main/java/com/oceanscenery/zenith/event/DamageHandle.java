package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ModConfigs;
import com.oceanscenery.zenith.registry.ModDamageType;
import com.oceanscenery.zenith.registry.ModItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@EventBusSubscriber
public class DamageHandle {
    private static boolean caughtException=false;

    @SubscribeEvent(priority= EventPriority.LOWEST)
    public static void onHurtEve(LivingDamageEvent.Pre event){
        if(!event.getEntity().level().isClientSide){
            LivingEntity victim = event.getEntity();
            if(!(event.getSource().getEntity() instanceof Player player)){
                return;
            }
            if (player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.ZENITH.get())) {
                if(victim instanceof Player){
                    float damage = event.getNewDamage();
                    if (damage < ModConfigs.getEnsuredDamageForPlayer()) {
                        event.setNewDamage(ModConfigs.getEnsuredDamageForPlayer());
                    }
                }else{
                    float damage = event.getNewDamage();
                    if (damage < ModConfigs.getEnsuredDamageForNonPlayer()) {
                        event.setNewDamage(ModConfigs.getEnsuredDamageForNonPlayer());
                    }
                }
            }
        }
    }

    public static boolean applyDamage(Player player,LivingEntity victim){
        return applyDamage(player,victim,1);
    }

    public static boolean applyDamage(Player player,LivingEntity victim,float multiply){
        if(!player.level().isClientSide){
            if(victim.isDeadOrDying()){
                return false;
            }
            boolean flag=false;
            float damage= player.getAttribute(Attributes.ATTACK_DAMAGE)==null ? 1:(float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            DamageSource source;
            if(ModConfigs.ZENITH_CONFIG.disable_knockback.get()){
                source=player.damageSources().source(ModDamageType.ZENITH,player);
            }else{
                source=player.damageSources().source(ModDamageType.ZENITH_KNOCKBACK,player);
            }
            EnchantmentHelper.doPostAttackEffectsWithItemSource((ServerLevel) player.level(), victim, source, source.getWeaponItem());
            damage=EnchantmentHelper.modifyDamage((ServerLevel) player.level(), source.getWeaponItem() == null ? player.getMainHandItem() : source.getWeaponItem(), victim, source, damage);
            damage=damage*(float)ModConfigs.getRangedFactor()*multiply;
            victim.invulnerableTime=0;
            if(ModConfigs.ZENITH_CONFIG.enable_bypass_invulnerable.get() && !caughtException){
                if(!damageProcess(victim,player,damage,source)){
                    victim.hurt(source,damage);
                }
            }else{
                victim.hurt(source,damage);
            }
            victim.invulnerableTime=0;
            return flag;
        }
        return false;
    }

    public static boolean damageProcess(LivingEntity victim,Player player,float damage,DamageSource source){
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(LivingEntity.class, MethodHandles.lookup());
            MethodHandles.Lookup extLookup=MethodHandles.privateLookupIn(victim.getClass(),MethodHandles.lookup());
            MethodHandle check_totem=lookup.findSpecial(LivingEntity.class,"checkTotemDeathProtection",MethodType.methodType(boolean.class,DamageSource.class), LivingEntity.class);
            MethodHandle getHealth=lookup.findStaticGetter(LivingEntity.class,"DATA_HEALTH_ID",EntityDataAccessor.class);
            MethodHandle hurtSound=extLookup.findSpecial(victim.getClass(),"playHurtSound",MethodType.methodType(void.class,DamageSource.class),victim.getClass());
            @SuppressWarnings("unchecked")
            EntityDataAccessor<Float> health_id=(EntityDataAccessor<Float>)getHealth.invoke();
            float pastHealth=victim.getEntityData().get(health_id);
            victim.getEntityData().set(health_id,pastHealth-damage);
            victim.setLastHurtByPlayer(player);
            victim.getCombatTracker().recordDamage(source,damage);
            victim.setLastHurtByPlayer(player);
            victim.hurtMarked=true;
            victim.level().broadcastDamageEvent(victim,source);
            hurtSound.invoke(victim,source);
            if(victim.isDeadOrDying()){
                if(!(boolean)check_totem.invoke(victim,source)){
                    victim.die(source);
                }
            }
        }catch (Throwable e) {
            TheZenithMod.LOGGER.error("an exception caught");
            caughtException=true;
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
