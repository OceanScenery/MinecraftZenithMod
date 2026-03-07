package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.registry.ModConfigs;
import com.oceanscenery.zenith.registry.ModDamageType;
import com.oceanscenery.zenith.registry.ModItems;
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
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

@EventBusSubscriber
public class DamageHandle {
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

    @SubscribeEvent
    public static void onHitShield(LivingShieldBlockEvent event){
        if(event.getDamageSource().getEntity() instanceof Player player){
            if(player.getMainHandItem().is(ModItems.ZENITH.get())){
                event.setCanceled(true);
            }
        }
    }

    public static boolean applyDamage(Player player,LivingEntity victim){
        return applyDamage(player,victim,1);
    }

    public static boolean applyDamage(Player player,LivingEntity victim,float multiply){
        if(!player.level().isClientSide){
            boolean flag;
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
            flag=victim.hurt(source,damage);
            victim.invulnerableTime=0;
            return flag;
        }
        return false;
    }
}
