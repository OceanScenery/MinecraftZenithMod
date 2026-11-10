package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.client.packet.SyncZenithSlotPacket;
import com.oceanscenery.zenith.mixin.accessor.EntityAccessor;
import com.oceanscenery.zenith.mixin_extension.IZenithEntityExtension;
import com.oceanscenery.zenith.registry.ZenithCapabilities;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_class.*;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.*;
import com.oceanscenery.zenith.zenith_class.saved_data.ZenithSlotData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
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
        ZenithHitMark.Serializable hitByZenithProvider = new ZenithHitMark.Serializable();
        ZenithHealthMark.Serializable zenithHealthProvider = new ZenithHealthMark.Serializable();
        ZenithDamageMark.Serializable damageMarkProvider = new ZenithDamageMark.Serializable();

        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "zenith_id_mark"), idMarkProvider);
        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "zenith_uuid_mark"), uuidMarkProvider);
        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "hit_by_zenith_mark"), hitByZenithProvider);
        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "zenith_health"), zenithHealthProvider);
        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "zenith_damage_mark"), damageMarkProvider);

        if (event.getObject() instanceof Player player) {
            ZenithSlot.Serializable slotProvider = new ZenithSlot.Serializable(player);
            event.addCapability(TheZenithMod.loc("zenith_slot"), slotProvider);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer tracker)) {
            return;
        }

        ZenithNetworkHandler.sendSnapshot(tracker, event.getTarget());

        if (event.getTarget() instanceof ServerPlayer target) {
            IZenithSlot slot = getSlot(target);
            if (slot != null) {
                ZenithNetworkHandler.playToClient(
                    new SyncZenithSlotPacket(
                        slot.valid(), slot.open(), slot.getDistance(), slot.getMode(), target.getUUID()
                    ),
                    tracker
                );
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        copyUuidMark(event.getOriginal(), event.getEntity());
        copyIdMark(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ZenithSlotData.fromCurrentServer().ifPresent(
                d -> d.mergeExist(player)
            );

            ZenithNetworkHandler.sendSnapshot(player, player);
            ZenithNetworkHandler.syncAllZenithSlotsTo(player);
            ZenithNetworkHandler.broadcastZenithSlot(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ZenithSlotData.fromCurrentServer().ifPresent(
                d -> d.mergeExist(player)
            );

            ZenithNetworkHandler.sendSnapshot(player, player);
            ZenithNetworkHandler.syncAllZenithSlotsTo(player);
            ZenithNetworkHandler.broadcastZenithSlot(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ZenithNetworkHandler.sendSnapshot(player, player);
            ZenithNetworkHandler.syncAllZenithSlotsTo(player);
            ZenithNetworkHandler.broadcastZenithSlot(player);
        }
    }

    @SubscribeEvent
    public static void serverTickCheck(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        boolean shouldCheck = false;
        for (ServerLevel serverLevel : event.getServer().getAllLevels()) {
            if (serverLevel.getGameTime() % 200 == 0) {
                shouldCheck = true;
            }
        }

        if (shouldCheck) {
            for (ServerLevel serverLevel : event.getServer().getAllLevels()) {
                for (ServerPlayer serverPlayer : serverLevel.players()) {
                    IZenithSlot slot = CapabilitiesHandler.getSlot(serverPlayer);
                    if (slot != null && slot.valid() != ZenithSlotData.playerValid(serverPlayer)) {
                        ZenithSlotData.fromCurrentServer().ifPresent(
                            d -> d.mergeExist(serverPlayer)
                        );
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onConversion(LivingConversionEvent.Post event) {
        copyIdMark(event.getEntity(), event.getOutcome());
        copyUuidMark(event.getEntity(), event.getOutcome());
    }

    public static void reviveCaps(Entity entity) {
        if (entity == null) {
            return;
        }
        entity.reviveCaps();
    }

    public static void checkRemoved(Entity entity) {
        if (entity == null) {
            return;
        }
        if (((EntityAccessor) entity).zenith$getRemovalReason() != null || IZenithEntityExtension.of(entity).zenith$removed()) {
            entity.invalidateCaps();
        }
    }

    public static IZenithUuidMark getUuidMark(Entity entity) {
        try {
            reviveCaps(entity);
            var optional = entity.getCapability(ZenithCapabilities.ZENITH_UUID_MARK);
            if (optional.isPresent() && optional.resolve().isPresent()) {
                return optional.resolve().get();
            }
            return null;
        } finally {
            checkRemoved(entity);
        }
    }

    public static IZenithIdMark getIdMark(Entity entity) {
        try {
            entity.reviveCaps();
            var optional = entity.getCapability(ZenithCapabilities.ZENITH_ID_MARK);
            if (optional.isPresent() && optional.resolve().isPresent()) {
                return optional.resolve().get();
            }
            return null;
        } finally {
            checkRemoved(entity);
        }
    }

    public static void copyIdMark(Entity origin, Entity replacement) {
        try {
            origin.reviveCaps();
            replacement.reviveCaps();

            IZenithIdMark from = getIdMark(origin);
            IZenithIdMark to = getIdMark(replacement);

            if (from == null || to == null) {
                return;
            }

            to.getIdSet().clear();
            to.getIdSet().addAll(from.getIdSet());
        } finally {
            checkRemoved(origin);
            checkRemoved(replacement);
        }
    }

    public static void copyUuidMark(Entity origin, Entity replacement) {
        try {
            origin.reviveCaps();
            replacement.reviveCaps();

            IZenithUuidMark from = getUuidMark(origin);
            IZenithUuidMark to = getUuidMark(replacement);

            if (from == null || to == null) {
                return;
            }

            to.getUuidSet().clear();
            to.getUuidSet().addAll(from.getUuidSet());
        } finally {
            checkRemoved(origin);
            checkRemoved(replacement);
        }
    }

    public static void toggleIdCap(Entity user, Entity target) {
        try {
            reviveCaps(user);
            reviveCaps(target);
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
        } finally {
            checkRemoved(user);
            checkRemoved(target);
        }
    }

    public static void toggleUuidCap(Entity user, Entity target) {
        try {
            reviveCaps(user);
            reviveCaps(target);
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
        } finally {
            checkRemoved(user);
            checkRemoved(target);
        }
    }

    public static boolean setBlacklist(Entity user, Entity target, boolean typeBlacklist, boolean blacklisted) {
        try {
            reviveCaps(user);
            reviveCaps(target);
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
        } finally {
            checkRemoved(user);
            checkRemoved(target);
        }
    }

    public static boolean setTypeBlacklist(Entity user, String typeId, boolean blacklisted) {
        try {
            reviveCaps(user);
            if (user == null || typeId == null) {
                return false;
            }

            IZenithIdMark mark = getIdMark(user);
            if (mark == null) {
                return false;
            }
            Set<String> idSet = mark.getIdSet();
            return blacklisted ? idSet.add(typeId) : idSet.remove(typeId);
        } finally {
            checkRemoved(user);
        }
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

        try {
            reviveCaps(attacker);
            reviveCaps(victim);

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
        } finally {
            checkRemoved(attacker);
            checkRemoved(victim);
        }
    }

    public static IZenithHitMark getHitMark(Entity entity) {
        try {
            reviveCaps(entity);
            var val = entity.getCapability(ZenithCapabilities.HIT_BY_ZENITH_MARK);
            if (val.isPresent() && val.resolve().isPresent()) {
                return val.resolve().get();
            }
            return null;
        } finally {
            checkRemoved(entity);
        }
    }

    public static void addDamageMark(LivingEntity livingVictim, float prevCheckedHealth, float postCheckedHealth, float damage, DamageSource source) {
        try {
            reviveCaps(livingVictim);
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
        try {
            reviveCaps(entity);
            IZenithHitMark mark = getHitMark(entity);
            if (mark == null) {
                return false;
            }
            return mark.valid(entity.level());
        } finally {
            checkRemoved(entity);
        }
    }

    public static void setHitByZenith(Entity entity, Function<Boolean, Boolean> newValueGetter) {
        setHitByZenith(entity, newValueGetter, 20);
    }

    public static void setHitByZenith(Entity entity, Function<Boolean, Boolean> newValueGetter, long persist) {
        setHitByZenith(entity, newValueGetter, entity.level().getGameTime(), persist);
    }

    public static void setHitByZenith(Entity entity, Function<Boolean, Boolean> newValue, long start, long persist) {
        try {
            reviveCaps(entity);
            IZenithHitMark mark = getHitMark(entity);
            boolean changed = false;
            if (mark != null) {
                if (mark.endTime() < start + persist) {
                    mark.set(start, start + persist, newValue.apply(mark.valid(entity.level())));
                    changed = true;
                } else if (!newValue.apply(mark.hit())) {
                    mark.set(0, 0, false);
                    changed = true;
                }
            }

            if (changed) {
                ZenithNetworkHandler.syncHitMark(entity);
            }
        } finally {
            checkRemoved(entity);
        }
    }

    public static IZenithDamageMark getMark(Entity entity) {
        try {
            reviveCaps(entity);
            var optional = entity.getCapability(ZenithCapabilities.ZENITH_DAMAGE_MARK);
            if (optional.isPresent() && optional.resolve().isPresent()) {
                return optional.resolve().get();
            }
            return null;
        } finally {
            checkRemoved(entity);
        }
    }

    public static IZenithHealthMark getHealth(Entity entity) {
        try {
            reviveCaps(entity);
            var optional = entity.getCapability(ZenithCapabilities.ZENITH_HEALTH_MARK);
            if (optional.isPresent() && optional.resolve().isPresent()) {
                return optional.resolve().get();
            }
            return null;
        } finally {
            checkRemoved(entity);
        }
    }

    public static void setHealth(Entity entity, float value) {
        try {
            reviveCaps(entity);
            IZenithHealthMark mark = getHealth(entity);
            if (mark != null) {
                mark.set(value, true);
            }

            ZenithNetworkHandler.syncHealthMark(entity);
        } finally {
            checkRemoved(entity);
        }
    }

    public static IZenithSlot getSlot(Player player) {
        if (player == null) {
            return null;
        }
        try {
            reviveCaps(player);
            return player.getCapability(ZenithCapabilities.ZENITH_SLOT).resolve().orElse(null);
        } finally {
            checkRemoved(player);
        }
    }
}
