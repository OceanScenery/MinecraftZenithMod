package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithCapabilities;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import com.oceanscenery.zenith.zenith_class.capabilities.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID)
public class CapabilitiesHandler {
    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event) {
        ZenithIdMarkSerializable idMarkProvider = new ZenithIdMarkSerializable();
        ZenithUuidMarkSerializable uuidMarkProvider = new ZenithUuidMarkSerializable();
        HitByZenithMark.Serializable hitByZenithProvider = new HitByZenithMark.Serializable();
        ZenithHealthMark.Serializable zenithHealthProvider = new ZenithHealthMark.Serializable();
        ZenithDamageMark.Serializable damageMarkProvider = new ZenithDamageMark.Serializable();

        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "zenith_id_mark"), idMarkProvider);
        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "zenith_uuid_mark"), uuidMarkProvider);
        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "hit_by_zenith_mark"), hitByZenithProvider);
        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "zenith_health"), zenithHealthProvider);
        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "zenith_damage_mark"), damageMarkProvider);

        event.addListener(idMarkProvider.idMarkOptional::invalidate);
        event.addListener(uuidMarkProvider.optional::invalidate);
        event.addListener(hitByZenithProvider.optional::invalidate);
        event.addListener(zenithHealthProvider.optional::invalidate);
        event.addListener(damageMarkProvider.optional::invalidate);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ZenithNetworkHandler.sendSnapshot(serverPlayer, event.getTarget());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ZenithNetworkHandler.sendSnapshot(player, player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ZenithNetworkHandler.sendSnapshot(player, player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        ServerPlayer original = (ServerPlayer) event.getOriginal();
        ServerPlayer replacement = (ServerPlayer) event.getEntity();

        original.reviveCaps();
        try {
            copyIdMark(original, replacement);
            copyUuidMark(original, replacement);
        } finally {
            original.invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onConversion(LivingConversionEvent.Post event) {
        copyIdMark(event.getEntity(), event.getOutcome());
        copyUuidMark(event.getEntity(), event.getOutcome());
    }

    public static IZenithUuidMark getUuidMark(Entity entity) {
        var optional = entity.getCapability(ZenithCapabilities.ZENITH_UUID_MARK);
        if (optional.isPresent() && optional.resolve().isPresent()) {
            return optional.resolve().get();
        }
        return null;
    }

    public static IZenithIdMark getIdMark(Entity entity) {
        var optional = entity.getCapability(ZenithCapabilities.ZENITH_ID_MARK);
        if (optional.isPresent() && optional.resolve().isPresent()) {
            return optional.resolve().get();
        }
        return null;
    }

    public static void copyIdMark(Entity origin, Entity replacement) {
        IZenithIdMark from = getIdMark(origin);
        IZenithIdMark to = getIdMark(replacement);

        if (from == null || to == null) {
            return;
        }

        to.getIdSet().clear();
        to.getIdSet().addAll(from.getIdSet());
    }

    public static void copyUuidMark(Entity origin, Entity replacement) {
        IZenithUuidMark from = getUuidMark(origin);
        IZenithUuidMark to = getUuidMark(replacement);

        if (from == null || to == null) {
            return;
        }

        to.getUuidSet().clear();
        to.getUuidSet().addAll(from.getUuidSet());
    }

    public static void toggleIdCap(Entity user, Entity target) {
        target = parentOrSelf(target);

        if (target == null || user == null) {
            return;
        }
        String id = EntityType.getKey(target.getType()).toString();
        var unsure = user.getCapability(ZenithCapabilities.ZENITH_ID_MARK);
        if (!unsure.isPresent() || unsure.resolve().isEmpty()) {
            return;
        }
        Set<String> idSet = unsure.resolve().get().getIdSet();

        if (idSet.contains(id)) {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("the_zenith_sword.message.remove_group_entities_to_list").append(":" + id),
                    true
                );
            }
            idSet.remove(id);
        } else {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("the_zenith_sword.message.add_group_entities_to_list").append(":" + id),
                    true
                );
            }
            idSet.add(id);
        }
    }

    public static void toggleUuidCap(Entity user, Entity target) {
        target = parentOrSelf(target);

        if (target == null || user == null) {
            return;
        }
        UUID id = user.getUUID();
        UUID targetId = target.getUUID();

        var unsure = target.getCapability(ZenithCapabilities.ZENITH_UUID_MARK);
        if (!unsure.isPresent() || unsure.resolve().isEmpty()) {
            return;
        }

        Set<UUID> uuidSet = unsure.resolve().get().getUuidSet();

        if (uuidSet.contains(id)) {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("the_zenith_sword.message.remove_single_entity_to_list").append(":" + targetId),
                    true
                );
            }
            uuidSet.remove(id);
        } else {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("the_zenith_sword.message.add_single_entity_to_list").append(":" + targetId),
                    true
                );
            }
            uuidSet.add(id);
        }
    }

    public static boolean setBlacklist(Entity user, Entity target, boolean typeBlacklist, boolean blacklisted) {
        if (user == null || target == null) {
            return false;
        }

        target = parentOrSelf(target);

        if (typeBlacklist) {
            return setTypeBlacklist(user, EntityType.getKey(target.getType()).toString(), blacklisted);
        }

        UUID id = user.getUUID();
        IZenithUuidMark mark = getUuidMark(target);
        if (mark == null) {
            return false;
        }
        Set<UUID> uuidSet = mark.getUuidSet();
        return blacklisted ? uuidSet.add(id) : uuidSet.remove(id);
    }

    public static boolean setTypeBlacklist(Entity user, String typeId, boolean blacklisted) {
        if (user == null || typeId == null) {
            return false;
        }

        IZenithIdMark mark = getIdMark(user);
        if (mark == null) {
            return false;
        }
        Set<String> idSet = mark.getIdSet();
        return blacklisted ? idSet.add(typeId) : idSet.remove(typeId);
    }

    public static Entity parentOrSelf(Entity target) {
        Entity resolved = target;
        while (resolved instanceof PartEntity<?> part) {
            resolved = part.getParent();
        }
        return resolved;
    }

    public static boolean checkCanAttack(Entity attacker, Entity victim) {
        if (attacker == null || victim == null) {
            return true;
        }

        victim = parentOrSelf(victim);

        String id = EntityType.getKey(victim.getType()).toString();
        UUID uuid = attacker.getUUID();

        var unsure1 = victim.getCapability(ZenithCapabilities.ZENITH_UUID_MARK);
        var unsure2 = attacker.getCapability(ZenithCapabilities.ZENITH_ID_MARK);

        if (!unsure1.isPresent() || unsure1.resolve().isEmpty()) {
            return true;
        }
        if (!unsure2.isPresent() || unsure2.resolve().isEmpty()) {
            return true;
        }

        Set<UUID> onVictim = unsure1.resolve().get().getUuidSet();
        Set<String> onAttacker = unsure2.resolve().get().getIdSet();

        if (onVictim.contains(uuid) || onAttacker.contains(id)) {
            return false;
        }
        return true;
    }

    public static IZenithHealthMark getHealthMark(Entity entity) {
        var val = entity.getCapability(ZenithCapabilities.ZENITH_HEALTH_MARK);
        if (val.isPresent() && val.resolve().isPresent()) {
            return val.resolve().get();
        }
        return null;
    }

    public static IHitByZenithMark getHitMark(Entity entity) {
        var val = entity.getCapability(ZenithCapabilities.HIT_BY_ZENITH_MARK);
        if (val.isPresent() && val.resolve().isPresent()) {
            return val.resolve().get();
        }
        return null;
    }

    public static void addDamageMark(LivingEntity livingVictim, float prevCheckedHealth, float postCheckedHealth, float damage, DamageSource source) {
        try {
            if (prevCheckedHealth - postCheckedHealth < damage) {
                IZenithDamageMark mark = getMark(livingVictim);

                if (mark != null) {
                    mark.getMap().merge(
                        source.getEntity().getUUID(),
                        damage - (prevCheckedHealth - postCheckedHealth),
                        Float::sum
                    );
                }
            }
        } catch (NullPointerException e) {
            TheZenithMod.LOGGER.error("A damageSource that should have none-null attacker has null attacker", e);
        }
    }

    public static boolean hitByZenith(Entity entity) {
        IHitByZenithMark mark = getHitMark(entity);
        if (mark == null) {
            return false;
        }
        return mark.valid(entity.level());
    }

    public static void setHitByZenith(Entity entity, Function<Boolean, Boolean> newValueGetter) {
        IHitByZenithMark mark = getHitMark(entity);
        if (mark != null) {
            mark.set(entity.level().getGameTime(), newValueGetter.apply(mark.valid(entity.level())));
        }

        ZenithNetworkHandler.syncHitMark(entity);
    }

    public static IZenithDamageMark getMark(Entity entity) {
        var optional = entity.getCapability(ZenithCapabilities.ZENITH_DAMAGE_MARK);
        if (optional.isPresent() && optional.resolve().isPresent()) {
            return optional.resolve().get();
        }
        return null;
    }

    public static IZenithHealthMark getHealth(Entity entity) {
        var optional = entity.getCapability(ZenithCapabilities.ZENITH_HEALTH_MARK);
        if (optional.isPresent() && optional.resolve().isPresent()) {
            return optional.resolve().get();
        }
        return null;
    }

    public static void setHealth(Entity entity, float value) {
        IZenithHealthMark mark = getHealth(entity);
        if (mark != null) {
            mark.set(value, entity.level().getGameTime(), true);
        }

        ZenithNetworkHandler.syncHealthMark(entity);
    }
}
