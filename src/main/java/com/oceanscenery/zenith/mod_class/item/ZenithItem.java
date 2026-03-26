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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
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
        return new Tool(List.of(),1.0F,0);
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
    public boolean canAttackBlock(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
            return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 0;
    }

    @Override
    public int getEnchantmentLevel(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        return super.getEnchantmentLevel(stack, enchantment);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity p_entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, p_entity, slotId, isSelected);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if(stack.get(ZenithDataComponents.DISTANCE)==null){
            stack.set(ZenithDataComponents.DISTANCE,new Distance(20));
        }
        if(stack.get(ZenithDataComponents.ATTACK_MODE)==null){
            stack.set(ZenithDataComponents.ATTACK_MODE,new AttackMode(AttackMode.Mode.LIVING_ENTITY,true));
        }
        double dist=stack.get(ZenithDataComponents.DISTANCE).dist();
        String atk=stack.get(ZenithDataComponents.ATTACK_MODE).getStrMode();
        boolean atkP=stack.get(ZenithDataComponents.ATTACK_MODE).attackPlayer();
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.distance.tooltip_pre").append(":"+dist));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.distance.tooltip_post"));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_mode.tooltip_pre").append(":"+atk));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_mode.tooltip_post"));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_player_mode.tooltip.pre").append(":"+atkP));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_player_mode.tooltip.post"));
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        return InteractionResult.FAIL;
    }

    public boolean canAttack(@NotNull ItemStack usedItem,@NotNull LivingEntity attacker){
        if(!attacker.level().isClientSide){
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

    public void attack(@NotNull ItemStack usedItem,LivingEntity attacker,Level level){
        if(this.canAttack(usedItem,attacker)){
            usedItem.set(ZenithDataComponents.LAST_USE_TIME,new LastUseTime(level.getGameTime()));
            Vector3[] relative=Vector3.getReferFromAngle(attacker.getXRot(),attacker.getYRot());
            Vec3 initial=new Vector3(0,0,100).VecInNewRefer(relative,Vector3.WORLD).toVec3();
            double distance;
            AABB box = new AABB(attacker.getEyePosition().add(initial.normalize().multiply(-1, -1, -1)), attacker.getEyePosition().add(initial)).inflate(1);
            List<Entity> list = level.getEntitiesOfClass(Entity.class, box, entity -> !(entity instanceof ZenithProjectile) && !entity.is(attacker));
            double farest=-1,nearest=200;
            ArrayList<Entity> victim = new ArrayList<>();

            for(Entity entity:list){
                AABB aabb=entity.getBoundingBox().inflate(1);
                java.util.Optional<Vec3> vec;
                if((vec=aabb.clip(attacker.getEyePosition(),attacker.getEyePosition().add(initial))).isPresent()){
                    if(DamageHandle.canAttack(entity,usedItem)){
                        victim.add(entity);
                        farest=Math.max(farest,vec.get().distanceTo(attacker.getEyePosition()));
                        nearest=Math.min(nearest,vec.get().distanceTo(attacker.getEyePosition()));
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
                distance=farest==-1?dist:farest;
            }else{
                distance=nearest==200?dist:nearest;
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

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(@NotNull ItemStack stack) {
        return 50;
    }

    public void addEntity(Level level, LivingEntity livingEntity, ItemStack stack, double distance, int type){
        if(!level.isClientSide && stack.is(ZenithItems.ZENITH)){
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
                    reference[0],
                    reference[1],
                    reference[2],
                    new Vector3(1,0,0),
                    new Vector3(0,1,0),
                    new Vector3(0,0,1)
            ).toVec3().add(livingEntity.getEyePosition()));

            Vector3 initial=new Vector3(0,0,1).VecInNewRefer(
                    reference[0],
                    reference[1],
                    reference[2],
                    new Vector3(1,0,0),
                    new Vector3(0,1,0),
                    new Vector3(0,0,1)
            );
            zenith.getEntityData().set(ZenithProjectile.FACING_VECTOR,initial);
            zenith.getEntityData().set(ZenithProjectile.LAST_VECTOR,initial);

            level.addFreshEntity(zenith);
        }
    }
}
