package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.mixin.accessor.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.entity.PartEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

public class EntityUtil {
    public static void commonRemove(Entity entity, Entity.RemovalReason reason) {
        try {
            if (entity.level().isClientSide()) {
                return;
            }
            entity.remove(reason);
            ((EntityAccessor) entity).zenith$setRemovalReason(reason);
            var levelCallback = ((EntityAccessor) entity).zenith$getLevelCallback();
            if (levelCallback != null && levelCallback != EntityInLevelCallback.NULL) {
                levelCallback.onRemove(reason);
                ((EntityAccessor) entity).zenith$setLevelCallback(EntityInLevelCallback.NULL);
            }
            entity.onRemovedFromWorld();
            entity.invalidateCaps();

            if (reason.shouldDestroy()) {
                entity.stopRiding();
            }
            for (Entity passenger : entity.getPassengers()) {
                passenger.stopRiding();
            }
        } catch (Throwable t) {
            TheZenithMod.LOGGER.error("Meet an throwable {} when trying to remove entity {}", t, entity.getUUID());
        }
    }

    public static void remove(Entity entity) {
        try {
            if (entity.level().isClientSide()) {
                return;
            }

            if (entity instanceof ServerPlayer serverPlayer) {
                Component component = null;
                if (serverPlayer.level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) {
                    Component component1 = Component.translatable("death.attack.message_too_long", Component.literal(
                        serverPlayer.getCombatTracker().getDeathMessage().getString(256)
                    ).withStyle(ChatFormatting.YELLOW));
                    component = Component.translatable("death.attack.even_more_magic", serverPlayer.getDisplayName()).withStyle((p_143420_) -> {
                        return p_143420_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component1));
                    });
                } else {
                    component = CommonComponents.EMPTY;
                }
                serverPlayer.connection.send(
                    new ClientboundPlayerCombatKillPacket(serverPlayer.getId(), component)
                );

                serverPlayer.setHealth(-1);
                CapabilitiesHandler.setHealth(serverPlayer, -1);
                CapabilitiesHandler.setHitByZenith(
                    serverPlayer, b -> true, Math.max(0, Long.MAX_VALUE - serverPlayer.level().getGameTime() - 1)
                );
            }

            commonRemove(entity, Entity.RemovalReason.KILLED);

            ServerLevel serverLevel = (ServerLevel) entity.level();
            var manager = ((ServerLevelAccessor) serverLevel).zenith$getEntityManager();

            var visibleLookup = ((PersistentEntitySectionManagerAccessor) manager).zenith$getVisibleEntityStorage();
            if (visibleLookup != null) {
                ((EntityLookupAccessor) visibleLookup).zenith$getById().remove(entity.getId());
                ((EntityLookupAccessor) visibleLookup).zenith$byUuid().remove(entity.getUUID());
            }

            EntitySectionStorage<?> sectionStorage = ((PersistentEntitySectionManagerAccessor) manager).zenith$getSectionStorage();

            if (sectionStorage != null) {
                SectionRef section = findSection(sectionStorage, entity);

                if (section != null) {
                    ((EntitySectionAccessor) section.section).zenith$getStorage().remove(entity);

                    var sectionStorageAc = ((EntitySectionStorageAccessor) sectionStorage);
                    if (section.section.isEmpty()) {
                        sectionStorageAc.zenith$getSections().remove(section.key);
                        sectionStorageAc.zenith$getSectionIds().remove(section.key);
                    }
                }
            }
            ((PersistentEntitySectionManagerAccessor) manager).zenith$getKnownUuids().remove(entity.getUUID());


            EntityTickList tickList = ((ServerLevelAccessor) serverLevel).zenith$getEntityTickList();

            ((EntityTickListAccessor) tickList).zenith$ensureActiveIsNotIterated();
            ((EntityTickListAccessor) tickList).zenith$getActive().remove(entity.getId());

            ServerChunkCache chunkCache = serverLevel.getChunkSource();
            chunkCache.removeEntity(entity);

            ((ChunkMapAccessor) ((ServerChunkCacheAccessor) chunkCache).zenith$getChunkMap()).zenith$getEntityMap().remove(entity.getId());
            ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entity.getId());
            for (ServerPlayer serverPlayer : serverLevel.players()) {
                serverPlayer.connection.send(packet);
            }

            serverLevel.getScoreboard().entityRemoved(entity);

            if (entity instanceof Mob mob) {
                ((ServerLevelAccessor) serverLevel).zenith$getNavigatingMobs().remove(mob);
            }

            Int2ObjectMap<PartEntity<?>> dragonParts = ((ServerLevelAccessor) serverLevel).zenith$getDragonParts();
            if (dragonParts != null) {
                Entity parent = entity instanceof PartEntity<?> part ? part.getParent() : entity;
                if (parent != null) {
                    dragonParts.remove(parent.getId());
                    for (PartEntity<?> part : parent.getParts()) {
                        if (part == null) {
                            continue;
                        }
                        dragonParts.remove(part.getId());
                    }
                }
            }

            clearBossBars(entity);
        } catch (Throwable t) {
            TheZenithMod.LOGGER.error("Failed to force remove entity.", t);
        }
    }

    private record SectionRef(long key, EntitySection<?> section) {
    }

    private static SectionRef findSection(EntitySectionStorage<?> sectionStorage, Entity entity) {
        Long2ObjectMap<EntitySection<?>> sections =
            ((EntitySectionStorageAccessor) sectionStorage).zenith$getSections();

        for (Long2ObjectMap.Entry<EntitySection<?>> entry : sections.long2ObjectEntrySet()) {
            EntitySection<?> section = entry.getValue();
            if (section == null) {
                continue;
            }

            var storage = ((EntitySectionAccessor) section).zenith$getStorage();
            if (storage.contains(entity)) {
                return new SectionRef(entry.getLongKey(), section);
            }
        }
        return null;
    }

    private static void clearBossBars(Entity entity) {
        for (Class<?> type = entity.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!BossEvent.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (value instanceof ServerBossEvent event) {
                        event.removeAllPlayers();
                        event.setVisible(false);
                    }
                } catch (Throwable t) {
                    TheZenithMod.LOGGER.info("caught an exception when scanning boss bar.", t);
                }
            }
        }
    }
}
