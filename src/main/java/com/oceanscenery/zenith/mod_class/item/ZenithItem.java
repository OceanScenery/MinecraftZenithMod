package com.oceanscenery.zenith.mod_class.item;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.event.DamageHandle;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.mod_class.data_component.Distance;
import com.oceanscenery.zenith.mod_class.data_component.LastUseTime;
import com.oceanscenery.zenith.mod_class.entity.ZenithProjectile;
import com.oceanscenery.zenith.registry.*;
import com.oceanscenery.zenith.tool.Vector3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class ZenithItem extends Item {
    public static final int TYPE_AMOUNT=21;
    public static final int USE_INTERVAL=4;
    public static final Random random=new Random();
    public static final boolean isBetterCombatLoaded= ModList.get().isLoaded("bettercombat");

    public ZenithItem(Properties properties) {
        super(properties);
    }

    public static Tool createToolProperties(){
        return new Tool(List.of(),2.0F,0,false);
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        return ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_ATTACK_DAMAGE_ID, ZenithConfigs.getDefaultDamage()*((TheZenithMod.TERRA_LOADED&&ZenithConfigs.ZENITH_CONFIG.enable_terra_damage_modifier.get())?24:1), AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,new AttributeModifier(BASE_ATTACK_SPEED_ID,isBetterCombatLoaded?-2.4:30, AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public float getAttackDamageBonus(@NotNull Entity target, float damage, @NotNull DamageSource damageSource) {
        return super.getAttackDamageBonus(target, damage, damageSource);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 0;
    }

    @Override
    public int getEnchantmentLevel(ItemInstance stack, Holder<Enchantment> enchantment) {
        return super.getEnchantmentLevel(stack, enchantment);
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(itemStack, level, owner, slot);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        return InteractionResult.FAIL;
    }

    public boolean canAttack(@NotNull ItemStack usedItem,@NotNull LivingEntity attacker){
        if(!attacker.level().isClientSide()){
            if(usedItem.get(ZenithDataComponents.LAST_USE_TIME)==null && usedItem.is(ZenithItems.ZENITH)){
                return true;
            }
            long last_tick=usedItem.get(ZenithDataComponents.LAST_USE_TIME).tickTime();
            long this_tick=attacker.level().getGameTime();
            if(this_tick>=last_tick+USE_INTERVAL || this_tick<last_tick){
                return true;
            }
            return false;
        }
        return false;
    }

    public void attack(@NotNull ItemStack usedItem, LivingEntity attacker, Level level, InteractionHand hand){
        if(this.canAttack(usedItem,attacker)){
            attacker.swing(hand,true);
            usedItem.set(ZenithDataComponents.LAST_USE_TIME,new LastUseTime(level.getGameTime()));
            Vector3[] relative=Vector3.getReferFromAngle(attacker.getXRot(),attacker.getYRot());
            Vec3 initial=new Vector3(0,0,100).VecInNewRefer(relative,Vector3.WORLD).toVec3();
            double distance;
            AABB box = new AABB(attacker.getEyePosition().add(initial.normalize().multiply(-1, -1, -1)), attacker.getEyePosition().add(initial)).inflate(1);
            List<Entity> list = level.getEntitiesOfClass(Entity.class, box, entity -> !(entity instanceof ZenithProjectile) && !entity.is(attacker));
            double farest=-1,nearest=200;
            ArrayList<Entity> victim = new ArrayList<>();

            for(Entity entity:list){
                AABB aabb=entity.getBoundingBox().inflate(2);
                if(aabb.clip(attacker.getEyePosition(),attacker.getEyePosition().add(initial)).isPresent()){
                    if(DamageHandle.canAttack(entity,usedItem)){
                        victim.add(entity);
                        Vec3 hit_pos=entity.getBoundingBox().getCenter();
                        farest=Math.max(farest,hit_pos.distanceTo(attacker.getEyePosition()));
                        nearest=Math.min(nearest,hit_pos.distanceTo(attacker.getEyePosition()));
                    }
                }
            }
            double dist;
            if(usedItem.get(ZenithDataComponents.DISTANCE)==null){
                usedItem.set(ZenithDataComponents.DISTANCE,new Distance(20));
                dist=20;
            }else{
                dist=usedItem.get(ZenithDataComponents.DISTANCE).dist();
            }

            if(ZenithConfigs.ZENITH_CONFIG.sort_farest.get()){
                distance=farest<0?dist:farest;
            }else{
                distance=nearest>150?dist:nearest;
            }
            if(distance<8){
                distance=8;
            }
            distance+=1;

            for(Entity entity:victim){
                DamageHandle.applyDamage(attacker,entity,usedItem,3);
            }

            Random random = new Random();
            int type1 = random.nextInt(1, TYPE_AMOUNT);
            int type2 = random.nextInt(1, TYPE_AMOUNT);

            this.addEntity(level,attacker,usedItem,distance,0);
            this.addEntity(level,attacker,usedItem,distance,type1);
            this.addEntity(level,attacker,usedItem,distance,type2);
        }
    }

    public void addEntity(Level level, LivingEntity livingEntity, ItemStack stack, double distance, int type){
        if(!level.isClientSide() && stack.is(ZenithItems.ZENITH)){
            ZenithProjectile zenith=new ZenithProjectile(
                    ZenithEntities.ZENITH_PROJECTILE.get(),
                    level,
                    livingEntity,
                    false,
                    Math.toRadians(random.nextFloat()*360),
                    type,
                    distance,
                    stack
            );
            Vector3[] reference=Vector3.getReferFromAngle(livingEntity.getXRot(),livingEntity.getYRot());

            zenith.setPos(new Vector3(0,0,-1).VecInNewRefer(
                    reference,
                    Vector3.WORLD
            ).toVec3().add(livingEntity.getEyePosition()));

            level.addFreshEntity(zenith);
        }
    }
}
