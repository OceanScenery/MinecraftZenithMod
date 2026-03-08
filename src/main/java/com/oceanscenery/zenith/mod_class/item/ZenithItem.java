package com.oceanscenery.zenith.mod_class.item;

import com.oceanscenery.zenith.event.DamageHandle;
import com.oceanscenery.zenith.mod_class.entity.ZenithProjectile;
import com.oceanscenery.zenith.registry.ModConfigs;
import com.oceanscenery.zenith.registry.ModEntity;
import com.oceanscenery.zenith.registry.ModItems;
import com.oceanscenery.zenith.tool.Vector3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.Platform;

import javax.sound.sampled.Clip;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class ZenithItem extends Item {
    public static final int TYPE_AMOUNT=21;
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
        return ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,new AttributeModifier(BASE_ATTACK_DAMAGE_ID,ModConfigs.getDefaultDamage(), AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,new AttributeModifier(BASE_ATTACK_SPEED_ID,isBetterCombatLoaded?-2.4:30, AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource damageSource) {
        return super.getAttackDamageBonus(target, damage, damageSource);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 1;
    }

    @Override
    public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.getEnchantmentLevel(stack, enchantment);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(!level.isClientSide && player.getMainHandItem().is(ModItems.ZENITH.get())){
            Vector3[] reference = Vector3.getReferFromAngle(player.getXRot(), player.getYRot());
            Vec3 initial = new Vector3(0, 0, 100).VecInNewRefer(
                    reference[0],
                    reference[1],
                    reference[2],
                    new Vector3(1, 0, 0),
                    new Vector3(0, 1, 0),
                    new Vector3(0, 0, 1)
            ).toVec3();
            double distance;

            AABB box = new AABB(player.getEyePosition().add(initial.normalize().multiply(-1,-1,-1)), player.getEyePosition().add(initial)).inflate(1);
            List<Entity> list = level.getEntitiesOfClass(Entity.class, box, entity -> !(entity instanceof ZenithProjectile) && !entity.is(player));
            double farest=-1,nearest=200,tmp;
            ArrayList<LivingEntity> victim = new ArrayList<>();
            BlockHitResult blockHitResult=level.clip(new ClipContext(player.getEyePosition(),player.getEyePosition().add(initial), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,player));
            double block_distance=blockHitResult.getLocation().distanceTo(player.getEyePosition());

            for (Entity entity : list) {
                AABB aabb=entity.getBoundingBox().inflate(1);
                if (aabb.clip(player.getEyePosition(), player.getEyePosition().add(initial)).isPresent()) {
                    if(entity instanceof LivingEntity && !((LivingEntity) entity).isDeadOrDying() && !(entity instanceof ArmorStand)){
                        if ((tmp = entity.distanceTo(player)) > farest) {
                            farest = tmp;
                        }
                        if ((tmp = entity.distanceTo(player)) < nearest) {
                            nearest = tmp;
                        }
                        victim.add((LivingEntity)entity);
                    }else if(entity.distanceTo(player)<block_distance){
                        player.attack(entity);
                    }
                }
            }
            if(ModConfigs.ZENITH_CONFIG.sort_farest.get()){
                distance=farest==-1?20:farest;
            }else{
                distance=nearest==200?20:nearest;
            }
            distance+=1;

            for (LivingEntity entity : victim) {
                DamageHandle.applyDamage(player,entity,3);
            }

            Random random=new Random();
            int type1=random.nextInt(0,TYPE_AMOUNT);
            int type2=random.nextInt(1,TYPE_AMOUNT);

            this.onUseTick(level, player, player.getItemInHand(usedHand), 1, distance,0);
            this.onUseTick(level, player, player.getItemInHand(usedHand), 1, distance,type1);
            this.onUseTick(level, player, player.getItemInHand(usedHand), 1, distance,type2);
        }else if(player.getMainHandItem().is(ModItems.ZENITH.get())){
            player.swing(InteractionHand.MAIN_HAND);
        }
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        //livingEntity.stopUsingItem();
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        onUseTick(level,livingEntity,stack,1,20,0);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 50;
    }

    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration, double distance, int type){
        if(!level.isClientSide && livingEntity instanceof Player player && player.getMainHandItem().is(ModItems.ZENITH.get())){
            ZenithProjectile zenith=new ZenithProjectile(
                    ModEntity.ZENITH_PROJECTILE.get(),
                    level,
                    player,
                    false,
                    Math.toRadians(random.nextFloat()*360),
                    type,
                    distance
            );
            Vector3[] reference=Vector3.getReferFromAngle(player.getXRot(),player.getYRot());

            zenith.setPos(new Vector3(0,0,-1).VecInNewRefer(
                    reference[0],
                    reference[1],
                    reference[2],
                    new Vector3(1,0,0),
                    new Vector3(0,1,0),
                    new Vector3(0,0,1)
            ).toVec3().add(player.getEyePosition()));

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
        }else{
            if(livingEntity.getMainHandItem().is(ModItems.ZENITH.get())){
                livingEntity.swing(InteractionHand.MAIN_HAND,true);
            }else {
                livingEntity.swing(InteractionHand.OFF_HAND,true);
            }
        }
    }
}
