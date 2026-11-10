package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.attachment.*;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.registry.ZenithAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class AttachmentUtil {
    public static void toggleIdAttachment(Entity user, Entity target) {
        if (target == null || user == null) {
            return;
        }
        target = parentOrSelf(target);
        String id = EntityType.getKey(target.getType()).toString();
        Set<String> idSet = user.getData(ZenithAttachments.ZENITH_ID_MARK).ids();

        if (idSet.contains(id)) {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("the_zenith_sword.message.remove_group_entities_to_list").append(":" + id),
                    true
                );
            }
            idSet.remove(id);
            user.setData(ZenithAttachments.ZENITH_ID_MARK, new ZenithIdMark(idSet));
        } else {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("the_zenith_sword.message.add_group_entities_to_list").append(":" + id),
                    true
                );
            }
            idSet.add(id);
            user.setData(ZenithAttachments.ZENITH_ID_MARK, new ZenithIdMark(idSet));
        }
    }

    public static void toggleUuidAttachment(Entity user, Entity target) {
        if (target == null || user == null) {
            return;
        }
        target = parentOrSelf(target);

        UUID id = user.getUUID();
        UUID targetId = target.getUUID();
        Set<UUID> uuidSet = target.getData(ZenithAttachments.ZENITH_PLAYER_MARK).uuids();

        if (uuidSet.contains(id)) {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("the_zenith_sword.message.remove_single_entity_to_list").append(":" + targetId),
                    true
                );
            }
            uuidSet.remove(id);
            target.setData(ZenithAttachments.ZENITH_PLAYER_MARK, new ZenithUuidMark(uuidSet));
        } else {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("the_zenith_sword.message.add_single_entity_to_list").append(":" + targetId),
                    true
                );
            }
            uuidSet.add(id);
            target.setData(ZenithAttachments.ZENITH_PLAYER_MARK, new ZenithUuidMark(uuidSet));
        }
    }

    public static boolean checkCanAttack(Entity attacker, Entity victim) {
        if (attacker == null || victim == null) {
            return true;
        }
        victim = parentOrSelf(victim);

        String id = EntityType.getKey(victim.getType()).toString();
        UUID uuid = attacker.getUUID();

        Set<UUID> onVictim = victim.getData(ZenithAttachments.ZENITH_PLAYER_MARK).uuids();
        Set<String> onAttacker = attacker.getData(ZenithAttachments.ZENITH_ID_MARK).ids();

        return !onVictim.contains(uuid) && !onAttacker.contains(id);
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
        Set<UUID> uuidSet = target.getData(ZenithAttachments.ZENITH_PLAYER_MARK).uuids();
        boolean changed = blacklisted ? uuidSet.add(id) : uuidSet.remove(id);
        if (changed) {
            target.setData(ZenithAttachments.ZENITH_PLAYER_MARK, new ZenithUuidMark(uuidSet));
        }
        return changed;
    }

    public static boolean setTypeBlacklist(Entity user, String typeId, boolean blacklisted) {
        if (user == null || typeId == null) {
            return false;
        }

        Set<String> idSet = user.getData(ZenithAttachments.ZENITH_ID_MARK).ids();
        boolean changed = blacklisted ? idSet.add(typeId) : idSet.remove(typeId);
        if (changed) {
            user.setData(ZenithAttachments.ZENITH_ID_MARK, new ZenithIdMark(idSet));
        }
        return changed;
    }

    public static Entity parentOrSelf(Entity target) {
        Entity resolved = target;
        while (resolved instanceof PartEntity<?> part) {
            resolved = part.getParent();
        }
        return resolved;
    }

    public static void addDamageMark(LivingEntity livingVictim, float prevCheckedHealth, float postCheckedHealth, float damage, DamageSource source) {
        try {
            if (prevCheckedHealth - postCheckedHealth < damage) {
                livingVictim.getData(ZenithAttachments.DAMAGE_MARK).getMap().merge(
                    source.getEntity().getUUID(),
                    damage - (prevCheckedHealth - postCheckedHealth),
                    Float::sum
                );
            }
        } catch (NullPointerException e) {
            TheZenithMod.LOGGER.error("A damageSource that should have none-null attacker has null attacker", e);
        }
    }

    public static boolean hitByZenith(Entity entity) {
        return entity.getExistingData(ZenithAttachments.HIT_BY_ZENITH_MARK).filter(
            h -> h.isActive(entity.level().getGameTime())
        ).isPresent();
    }

    public static ZenithHitMark zenithHitMark(Entity entity) {
        return entity.getData(ZenithAttachments.HIT_BY_ZENITH_MARK);
    }

    public static void setHitByZenith(Entity entity, Function<Boolean, Boolean> newValueGetter) {
        setHitByZenith(entity, newValueGetter, 20);
    }

    public static void setHitByZenith(Entity entity, Function<Boolean, Boolean> newValue, long duration) {
        setHitByZenith(entity, newValue, entity.level().getGameTime(), duration);
    }

    public static void setHitByZenith(Entity entity, Function<Boolean, Boolean> newValue, long start, long duration) {
        ZenithHitMark hit = entity.getData(ZenithAttachments.HIT_BY_ZENITH_MARK);
        if (hit.expireTime() < start + duration) {
            entity.setData(
                ZenithAttachments.HIT_BY_ZENITH_MARK,
                new ZenithHitMark(
                    newValue.apply(hitByZenith(entity)),
                    start,
                    start + duration
                )
            );
        } else if (!newValue.apply(hit.hit())) {
            entity.setData(
                ZenithAttachments.HIT_BY_ZENITH_MARK,
                new ZenithHitMark(
                    false,
                    0,
                    0
                )
            );
        }
    }

    public static ZenithDamageMark getMark(Entity entity) {
        return entity.getData(ZenithAttachments.DAMAGE_MARK);
    }

    public static Optional<ZenithHealthMark> getExistingHealth(Entity entity) {
        return entity.getExistingData(ZenithAttachments.ZENITH_HEALTH_MARK);
    }

    public static ZenithHealthMark getHealth(Entity entity) {
        return entity.getData(ZenithAttachments.ZENITH_HEALTH_MARK);
    }

    public static void setHealth(Entity entity, float value) {
        entity.setData(ZenithAttachments.ZENITH_HEALTH_MARK, new ZenithHealthMark(value, true));
    }

    public static ZenithSlot getSlot(Player player) {
        return player.getData(ZenithAttachments.ZENITH_SLOT);
    }

    public static void validateSlot(Player player) {
        var origin = player.getData(ZenithAttachments.ZENITH_SLOT);
        player.setData(
            ZenithAttachments.ZENITH_SLOT,
            ZenithSlot.Builder.of()
                .mode(origin.mode()).valid(true).open(origin.valid())
                .distance(origin.distanceAsInt()).build()
        );
    }

    public static void toggleSlotOpen(Player player, Function<Boolean, Boolean> openBehave) {
        ZenithSlot curr = player.getData(ZenithAttachments.ZENITH_SLOT);
        if (!curr.valid()) {
            return;
        }

        player.setData(
            ZenithAttachments.ZENITH_SLOT,
            ZenithSlot.Builder.of()
                .mode(curr.mode()).valid(true).open(openBehave.apply(curr.open()))
                .distance(curr.distanceAsInt()).build()
        );
    }

    public static void toggleSlotDistance(Player player) {
        ZenithSlot curr = player.getData(ZenithAttachments.ZENITH_SLOT);
        if (!curr.valid()) {
            return;
        }

        player.setData(
            ZenithAttachments.ZENITH_SLOT,
            ZenithSlot.Builder.of()
                .mode(curr.mode()).valid(true).open(curr.open())
                .distance(curr.distanceAsInt() % 100 + 20).build()
        );
    }

    public static void toggleSlotMode(Player player) {
        ZenithSlot curr = player.getData(ZenithAttachments.ZENITH_SLOT);
        if (!curr.valid()) {
            return;
        }

        AttackMode next;

        switch (curr.mode()) {
            case LIVING_ENTITY -> next = AttackMode.ATTACKABLE_ENTITY;
            case ATTACKABLE_ENTITY -> next = AttackMode.ALL;
            default -> next = AttackMode.LIVING_ENTITY;
        }

        player.setData(
            ZenithAttachments.ZENITH_SLOT,
            ZenithSlot.Builder.of()
                .mode(next).valid(true).open(curr.open())
                .distance(curr.distanceAsInt()).build()
        );
    }
}
