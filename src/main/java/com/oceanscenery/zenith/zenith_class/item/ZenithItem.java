package com.oceanscenery.zenith.zenith_class.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.oceanscenery.zenith.event.DamageHandler;
import com.oceanscenery.zenith.event.WeaponHandler;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithEntities;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.tool.Vector3;
import com.oceanscenery.zenith.zenith_class.entity.ZenithProjectile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ZenithItem extends Item {
    public static final int TYPE_AMOUNT=21;
    public static final int USE_INTERVAL=4;
    public static final Random random=new Random();
    public static final boolean isBetterCombatLoaded=ModList.get().isLoaded("bettercombat");

    public ZenithItem(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if(slot==EquipmentSlot.MAINHAND){
            return ImmutableMultimap.of(
                    Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "zenith", ZenithConfigs.ZENITH_CONFIG.default_damage.get(), AttributeModifier.Operation.ADDITION),
                    Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "zenith", isBetterCombatLoaded?-2.4:30, AttributeModifier.Operation.ADDITION)
            );
        }
        return ImmutableMultimap.of();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 50;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltipComponents, flag);

        double dist=WeaponHandler.getDistance(stack);
        int mode=WeaponHandler.getAttackMode(stack);
        String atk=mode==0?"living_entity":mode==1?"attackable_entity":"all";
        boolean atkP=WeaponHandler.attackPlayer(stack);

        tooltipComponents.add(Component.translatable("the_zenith_sword.item.distance.tooltip_pre").append(":"+dist));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.distance.tooltip_post"));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_mode.tooltip_pre").append(":"+atk));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_mode.tooltip_post"));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_player_mode.tooltip.pre").append(":"+atkP));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_player_mode.tooltip.post"));
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack=super.getDefaultInstance();
        CompoundTag tag=stack.getOrCreateTag();
        CompoundTag z_tag=new CompoundTag();
        z_tag.putLong("use_tick",0);
        z_tag.putDouble("distance",20);
        z_tag.putInt("attack_mode",0);
        z_tag.putBoolean("attack_player",true);
        tag.put("Zenith",z_tag);
        stack.setTag(tag);
        return stack;
    }

    public boolean canAttack(@NotNull ItemStack usedItem,@NotNull LivingEntity attacker){
        if(!attacker.level().isClientSide()){
            long last_tick=WeaponHandler.getLastUseTick(usedItem);
            long this_tick=attacker.level().getGameTime();
            if(this_tick>=last_tick+USE_INTERVAL || this_tick<last_tick){
                return true;
            }
            return false;
        }
        return false;
    }

    public void attack(@NotNull ItemStack usedItem, LivingEntity attacker, Level level,InteractionHand hand){
        if(this.canAttack(usedItem,attacker)){
            attacker.swing(hand,true);
            CompoundTag tag=usedItem.getOrCreateTag();
            WeaponHandler.checkExist(tag);
            tag.getCompound("Zenith").putLong("use_tick",level.getGameTime());
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
                    if(DamageHandler.canAttack(entity,usedItem)){
                        Vec3 vec=entity.getBoundingBox().getCenter();
                        victim.add(entity);
                        farest=Math.max(farest,vec.distanceTo(attacker.getEyePosition()));
                        nearest=Math.min(nearest,vec.distanceTo(attacker.getEyePosition()));
                    }
                }
            }
            double dist=WeaponHandler.getDistance(usedItem);

            if(ZenithConfigs.ZENITH_CONFIG.sort_farest.get()){
                distance=farest==-1?dist:farest;
            }else{
                distance=nearest==200?dist:nearest;
            }

            if(distance<8){
                distance=8;
            }

            distance+=1;

            for(Entity entity:victim){
                DamageHandler.applyDamage(attacker,entity,usedItem,3);
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
        if(!level.isClientSide && stack.is(ZenithItems.ZENITH.get())){
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
