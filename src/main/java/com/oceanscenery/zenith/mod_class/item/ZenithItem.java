package com.oceanscenery.zenith.mod_class.item;

import com.oceanscenery.zenith.event.ServerTickTask;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.mod_class.data_component.Distance;
import com.oceanscenery.zenith.mod_class.data_component.LastUseTime;
import com.oceanscenery.zenith.mod_class.entity.ZenithProjectile;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithDataComponents;
import com.oceanscenery.zenith.registry.ZenithEntities;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.util.ConfigUtil;
import com.oceanscenery.zenith.util.DamageHandler;
import com.oceanscenery.zenith.util.StaticCollections;
import com.oceanscenery.zenith.util.Vector3;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ZenithItem extends Item {
    public static final int TYPE_AMOUNT = 21;
    public static final int USE_INTERVAL = 4;
    public static final Random random = new Random();

    public ZenithItem(Properties properties) {
        super(properties);
    }

    public static Tool createToolProperties() {
        return new Tool(List.of(), 1.0F, 0, false);
    }

    @Override
    public @NotNull ItemAttributeModifiers getDefaultAttributeModifiers(@NotNull ItemStack stack) {
        return ConfigUtil.defaultAttributeModifiersByConfig();
    }


    @Override
    public @NonNull ItemUseAnimation getUseAnimation(@NonNull ItemStack itemStack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 0;
    }

    @Override
    public int getEnchantmentLevel(@NonNull ItemInstance stack, @NonNull Holder<Enchantment> enchantment) {
        return super.getEnchantmentLevel(stack, enchantment);
    }

    @Override
    public void inventoryTick(@NonNull ItemStack itemStack, @NonNull ServerLevel level, @NonNull Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(itemStack, level, owner, slot);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        return InteractionResult.FAIL;
    }

    public boolean canAttack(@NotNull ItemStack usedItem, @NotNull LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {
            LastUseTime lastUseTime = usedItem.get(ZenithDataComponents.LAST_USE_TIME);
            if (lastUseTime == null) {
                return true;
            }
            long lastTick = lastUseTime.tickTime();
            long thisTick = attacker.level().getGameTime();
            return thisTick >= lastTick + USE_INTERVAL || thisTick < lastTick;
        }
        return false;
    }

    public void attack(@NotNull ItemStack usedItem, LivingEntity attacker, Level level, InteractionHand hand) {
        if (this.canAttack(usedItem, attacker)) {
            attacker.swing(hand, SwingAnimation.DEFAULT, true);
            usedItem.set(ZenithDataComponents.LAST_USE_TIME, new LastUseTime(level.getGameTime()));
            Vector3[] relative = Vector3.getReferFromAngle(attacker.getXRot(), attacker.getYRot());
            Vec3 initial = new Vector3(0, 0, 100).VecInNewRefer(relative, Vector3.WORLD).toVec3();
            double distance;
            AABB box = new AABB(attacker.getEyePosition().add(initial.normalize().multiply(-1, -1, -1)), attacker.getEyePosition().add(initial)).inflate(1);
            List<Entity> list = level.getEntitiesOfClass(Entity.class, box, entity -> !(entity instanceof ZenithProjectile) && !entity.is(attacker));
            double farest = -1, nearest = 200;
            ArrayList<Entity> victim = new ArrayList<>();

            for (Entity entity : list) {
                AABB aabb = entity.getBoundingBox().inflate(2);
                if (aabb.clip(attacker.getEyePosition(), attacker.getEyePosition().add(initial)).isPresent()) {
                    if (DamageHandler.DEFAULT_FILTER.canAttack(attacker, entity, usedItem)) {
                        victim.add(entity);
                        Vec3 hitPos = entity.getBoundingBox().getCenter();
                        farest = Math.max(farest, hitPos.distanceTo(attacker.getEyePosition()));
                        nearest = Math.min(nearest, hitPos.distanceTo(attacker.getEyePosition()));
                    }
                }
            }
            double dist;
            if (usedItem.get(ZenithDataComponents.DISTANCE) == null) {
                usedItem.set(ZenithDataComponents.DISTANCE, new Distance(20));
                dist = 20;
            } else {
                dist = usedItem.get(ZenithDataComponents.DISTANCE).dist();
            }

            if (ZenithConfigs.ZENITH_CONFIG.sort_farest.get()) {
                distance = farest < 0 ? dist : farest;
            } else {
                distance = nearest > 150 ? dist : nearest;
            }
            if (distance < 8) {
                distance = 8;
            }
            distance += 1;

            for (Entity entity : victim) {
                DamageHandler.applyDirectDamage(attacker, entity, usedItem, 3, (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }

            Random random = new Random();
            int type1 = random.nextInt(1, TYPE_AMOUNT);
            int type2 = random.nextInt(1, TYPE_AMOUNT);

            this.addEntity(level, attacker, usedItem, distance, 0);

            double finalDistance = distance;
            StaticCollections.taskList.add(new ServerTickTask(
                2,
                () -> this.addEntity(level, attacker, usedItem, finalDistance, type1)
            ));
            StaticCollections.taskList.add(new ServerTickTask(
                4,
                () -> this.addEntity(level, attacker, usedItem, finalDistance, type2)
            ));
        }
    }

    public void addEntity(Level level, LivingEntity livingEntity, ItemStack stack, double distance, int type) {
        if (!level.isClientSide() && stack.is(ZenithItems.ZENITH)) {
            ZenithProjectile zenith = new ZenithProjectile(
                ZenithEntities.ZENITH_PROJECTILE.get(),
                level,
                livingEntity,
                false,
                Math.toRadians(random.nextFloat() * 360),
                type,
                distance,
                stack
            );
            Vector3[] reference = Vector3.getReferFromAngle(livingEntity.getXRot(), livingEntity.getYRot());

            zenith.setPos(new Vector3(0, 0, -1).VecInNewRefer(
                reference,
                Vector3.WORLD
            ).toVec3().add(livingEntity.getEyePosition()));
            zenith.setDamage((float) livingEntity.getAttributeValue(Attributes.ATTACK_DAMAGE));

            level.addFreshEntity(zenith);
        }
    }

    @Override
    public @NonNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.set(ZenithDataComponents.ATTACK_MODE, AttackMode.LIVING_ENTITY);
        stack.set(ZenithDataComponents.DISTANCE, new Distance(20));
        stack.set(ZenithDataComponents.LAST_USE_TIME, new LastUseTime(0));
        return stack;
    }

    public @NotNull ItemStack instanceOf(int distance, AttackMode attackMode) {
        ItemStack stack = new ItemStack(this);
        if (distance % 20 != 0 || distance <= 0 || distance > 100) {
            distance = 20;
        }
        stack.set(ZenithDataComponents.LAST_USE_TIME, new LastUseTime(0));
        stack.set(ZenithDataComponents.DISTANCE, new Distance(distance));
        stack.set(ZenithDataComponents.ATTACK_MODE, attackMode);
        return stack;
    }
}
