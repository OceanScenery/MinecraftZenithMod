package com.oceanscenery.zenith.zenith_class.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.event.ServerTickTask;
import com.oceanscenery.zenith.event.WeaponHandler;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithEntities;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.util.DamageHandler;
import com.oceanscenery.zenith.util.DamageHandler.AttackFilter;
import com.oceanscenery.zenith.util.PosUtil;
import com.oceanscenery.zenith.util.StaticCollections;
import com.oceanscenery.zenith.util.Vector3;
import com.oceanscenery.zenith.zenith_class.entity.ZenithProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private static boolean isBetterCombatLoaded() {
        return ModList.get().isLoaded("bettercombat");
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            return ImmutableMultimap.of(
                Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "zenith", ZenithConfigs.getDefaultDamage() * ((TheZenithMod.TERRA_LOADED && ZenithConfigs.ZENITH_CONFIG.enable_terra_damage_modifier.get()) ? 24 : 1), AttributeModifier.Operation.ADDITION),
                Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "zenith", isBetterCombatLoaded() ? -2.4 : 30, AttributeModifier.Operation.ADDITION)
            );
        }
        return ImmutableMultimap.of();
    }

    @Override
    public boolean canAttackBlock(BlockState p_41441_, Level p_41442_, BlockPos p_41443_, Player p_41444_) {
        return !p_41444_.isCreative();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 50;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltipComponents, flag);

        double dist = WeaponHandler.getDistance(stack);
        int mode = WeaponHandler.getAttackMode(stack);
        String atk = mode == 0 ? "living_entity" : mode == 1 ? "attackable_entity" : "all";
        boolean atkP = WeaponHandler.attackPlayer(stack);

        tooltipComponents.add(Component.translatable("the_zenith_sword.item.distance.tooltip_pre").append(":" + dist));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.distance.tooltip_post"));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_mode.tooltip_pre").append(":" + atk));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_mode.tooltip_post"));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_player_mode.tooltip.pre").append(":" + atkP));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_player_mode.tooltip.post"));
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag z_tag = new CompoundTag();
        z_tag.putLong("use_tick", 0);
        z_tag.putDouble("distance", 20);
        z_tag.putInt("attack_mode", 0);
        z_tag.putBoolean("attack_player", true);
        tag.put("Zenith", z_tag);
        stack.setTag(tag);
        return stack;
    }

    public boolean canAttack(@NotNull ItemStack usedItem, @NotNull LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {
            long last_tick = WeaponHandler.getLastUseTick(usedItem);
            long this_tick = attacker.level().getGameTime();
            if (this_tick >= last_tick + USE_INTERVAL || this_tick < last_tick) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void attack(@NotNull ItemStack usedItem, LivingEntity attacker, Level level, InteractionHand hand) {
        this.attack(usedItem, attacker, level, hand, DamageHandler.DEFAULT_FILTER);
    }

    public void attack(@NotNull ItemStack usedItem, LivingEntity attacker, Level level, InteractionHand hand, AttackFilter filter) {
        if (this.canAttack(usedItem, attacker)) {
            attacker.swing(hand, true);
            CompoundTag tag = usedItem.getOrCreateTag();
            WeaponHandler.checkExist(tag);
            tag.getCompound("Zenith").putLong("use_tick", level.getGameTime());
            Vector3[] relative = Vector3.getReferFromAngle(attacker .getXRot(), attacker.getYRot());
            Vec3 initial = new Vector3(0, 0, 100).VecInNewRefer(relative, Vector3.WORLD).toVec3();
            double distance;
            AABB box = new AABB(attacker.getEyePosition().add(initial.normalize().multiply(-1, -1, -1)), attacker.getEyePosition().add(initial)).inflate(1);
            List<Entity> list = level.getEntitiesOfClass(Entity.class, box, entity -> !(entity instanceof ZenithProjectile) && !entity.is(attacker));
            double farest = -1, nearest = 200;
            ArrayList<Entity> victim = new ArrayList<>();

            for (Entity entity : list) {
                AABB aabb = entity.getBoundingBox().inflate(2);
                if (aabb.clip(attacker.getEyePosition(), attacker.getEyePosition().add(initial)).isPresent()) {
                    if (filter.canAttack(attacker, entity, usedItem)) {
                        Vec3 vec = entity.getBoundingBox().getCenter();
                        victim.add(entity);
                        farest = Math.max(farest, vec.distanceTo(attacker.getEyePosition()));
                        nearest = Math.min(nearest, vec.distanceTo(attacker.getEyePosition()));
                    }
                }
            }
            double dist = WeaponHandler.getDistance(usedItem);

            if (ZenithConfigs.ZENITH_CONFIG.sort_farest.get()) {
                distance = farest == -1 ? dist : farest;
            } else {
                distance = nearest == 200 ? dist : nearest;
            }

            if (distance < 8) {
                distance = 8;
            }

            distance += 1;

            for (Entity entity : victim) {
                DamageHandler.applyDirectDamage(attacker, entity, usedItem, 3, (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }

            int type1 = random.nextInt(1, TYPE_AMOUNT);
            int type2 = random.nextInt(1, TYPE_AMOUNT);

            this.addEntity(level, attacker, usedItem, distance, 0,filter);
            double finalDistance = distance;
            StaticCollections.taskList.add(new ServerTickTask(
                2,
                () -> this.addEntity(level, attacker, usedItem, finalDistance, type1,filter)
            ));
            StaticCollections.taskList.add(new ServerTickTask(
                4,
                () -> this.addEntity(level, attacker, usedItem, finalDistance, type2,filter)
            ));
        }
    }

    public void attackWithTarget(ItemStack usedItem, LivingEntity attacker, Entity target, Level level, InteractionHand hand, AttackFilter filter) {
        if (this.canAttack(usedItem, attacker)) {
            double distance = target.getBoundingBox().getCenter().distanceTo(attacker.getEyePosition());
            if (distance > 100) {
                return;
            }

            attacker.swing(hand, true);
            CompoundTag tag = usedItem.getOrCreateTag();
            WeaponHandler.checkExist(tag);
            tag.getCompound("Zenith").putLong("use_tick", level.getGameTime());
            Vector3[] relative = Vector3.getReferFromVec(
                Vector3.of(target.getBoundingBox().getCenter().subtract(attacker.getEyePosition()))
            );
            Vec3 initial = new Vector3(0, 0, 100).VecInNewRefer(relative, Vector3.WORLD).toVec3();
            AABB box = new AABB(attacker.getEyePosition().add(initial.normalize().multiply(-1, -1, -1)), attacker.getEyePosition().add(initial)).inflate(1);
            List<Entity> list = level.getEntitiesOfClass(Entity.class, box, entity -> !(entity instanceof ZenithProjectile) && !entity.is(attacker));
            ArrayList<Entity> victim = new ArrayList<>();

            for (Entity entity : list) {
                AABB aabb = entity.getBoundingBox().inflate(2);
                if (aabb.clip(attacker.getEyePosition(), attacker.getEyePosition().add(initial)).isPresent()) {
                    if (filter.canAttack(attacker, entity, usedItem)) {
                        victim.add(entity);
                    }
                }
            }

            if (distance < 8) {
                distance = 8;
            }

            distance += 1;

            for (Entity entity : victim) {
                DamageHandler.applyDirectDamage(attacker, entity, usedItem, 3, (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }

            int type1 = random.nextInt(1, TYPE_AMOUNT);
            int type2 = random.nextInt(1, TYPE_AMOUNT);

            this.addEntity(level, attacker, target, usedItem, distance, 0,filter);
            double finalDistance = distance;
            StaticCollections.taskList.add(new ServerTickTask(
                2,
                () -> this.addEntity(level, attacker, target, usedItem, finalDistance, type1,filter)
            ));
            StaticCollections.taskList.add(new ServerTickTask(
                4,
                () -> this.addEntity(level, attacker, target, usedItem, finalDistance, type2,filter)
            ));
        }
    }

    public void addEntity(Level level, LivingEntity livingEntity, ItemStack stack, double distance, int type,AttackFilter filter) {
        if (!level.isClientSide && stack.is(ZenithItems.ZENITH.get())) {
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
            zenith.setFilter(filter);
            Vector3[] reference = Vector3.getReferFromAngle(livingEntity.getXRot(), livingEntity.getYRot());

            zenith.setPos(new Vector3(0, 0, -1).VecInNewRefer(
                reference,
                Vector3.WORLD
            ).toVec3().add(livingEntity.getEyePosition()));
            zenith.setDamage((float) livingEntity.getAttributeValue(Attributes.ATTACK_DAMAGE));

            level.addFreshEntity(zenith);
        }
    }

    public void addEntity(Level level, LivingEntity attacker, Entity target, ItemStack stack, double distance, int type,AttackFilter filter) {
        if (level.isClientSide || !stack.is(ZenithItems.ZENITH.get())) {
            return;
        }

        ZenithProjectile zenith = new ZenithProjectile(
            ZenithEntities.ZENITH_PROJECTILE.get(),
            level,
            attacker,
            false,
            Math.toRadians(random.nextFloat() * 360),
            type,
            distance,
            stack
        );
        zenith.setFilter(filter);

        Vec3 aimDirection = target.getBoundingBox().getCenter().subtract(attacker.getEyePosition());
        Vector3[] ref = Vector3.getReferFromVec(Vector3.of(aimDirection));
        zenith.setPos(
            new Vector3(0, 0, -1).VecInNewRefer(
                ref, Vector3.WORLD
            ).toVec3().add(attacker.getEyePosition())
        );
        zenith.setIniRot(PosUtil.rotationFromDirection(aimDirection));
        zenith.setDamage((float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));

        level.addFreshEntity(zenith);
    }
}
