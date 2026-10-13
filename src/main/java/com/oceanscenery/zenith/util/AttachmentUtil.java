package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.attachment.DamageMark;
import com.oceanscenery.zenith.mod_class.attachment.HitByZenithMark;
import com.oceanscenery.zenith.mod_class.attachment.ZenithHealthMark;
import com.oceanscenery.zenith.registry.ZenithAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.entity.PartEntity;

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
        Set<String> idSet = user.getData(ZenithAttachments.ZENITH_ID_MARK);

        if (idSet.contains(id)) {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("the_zenith_sword.message.remove_group_entities_to_list").append(":" + id),
                    true
                );
            }
            idSet.remove(id);
            user.setData(ZenithAttachments.ZENITH_ID_MARK, idSet);
        } else {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("the_zenith_sword.message.add_group_entities_to_list").append(":" + id),
                    true
                );
            }
            idSet.add(id);
            user.setData(ZenithAttachments.ZENITH_ID_MARK, idSet);
        }
    }

    public static void toggleUuidAttachment(Entity user, Entity target) {
        if (target == null || user == null) {
            return;
        }
        target = parentOrSelf(target);

        UUID id = user.getUUID();
        UUID targetId = target.getUUID();
        Set<UUID> uuidSet = target.getData(ZenithAttachments.ZENITH_PLAYER_MARK);

        if (uuidSet.contains(id)) {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("the_zenith_sword.message.remove_single_entity_to_list").append(":" + targetId),
                    true
                );
            }
            uuidSet.remove(id);
            target.setData(ZenithAttachments.ZENITH_PLAYER_MARK, uuidSet);
        } else {
            if (user instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("the_zenith_sword.message.add_single_entity_to_list").append(":" + targetId),
                    true
                );
            }
            uuidSet.add(id);
            target.setData(ZenithAttachments.ZENITH_PLAYER_MARK, uuidSet);
        }
    }

    public static boolean checkCanAttack(Entity attacker, Entity victim) {
        if (attacker == null || victim == null) {
            return true;
        }
        victim = parentOrSelf(victim);

        String id = EntityType.getKey(victim.getType()).toString();
        UUID uuid = attacker.getUUID();

        Set<UUID> onVictim = victim.getData(ZenithAttachments.ZENITH_PLAYER_MARK);
        Set<String> onAttacker = attacker.getData(ZenithAttachments.ZENITH_ID_MARK);

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
        Set<UUID> uuidSet = target.getData(ZenithAttachments.ZENITH_PLAYER_MARK);
        boolean changed = blacklisted ? uuidSet.add(id) : uuidSet.remove(id);
        if (changed) {
            target.setData(ZenithAttachments.ZENITH_PLAYER_MARK, uuidSet);
        }
        return changed;
    }

    public static boolean setTypeBlacklist(Entity user, String typeId, boolean blacklisted) {
        if (user == null || typeId == null) {
            return false;
        }

        Set<String> idSet = user.getData(ZenithAttachments.ZENITH_ID_MARK);
        boolean changed = blacklisted ? idSet.add(typeId) : idSet.remove(typeId);
        if (changed) {
            user.setData(ZenithAttachments.ZENITH_ID_MARK, idSet);
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
        return entity.getData(ZenithAttachments.HIT_BY_ZENITH_MARK).isActive(entity.level().getGameTime());
    }

    public static void setHitByZenith(Entity entity, Function<Boolean, Boolean> newValueGetter) {
        entity.setData(
            ZenithAttachments.HIT_BY_ZENITH_MARK,
            new HitByZenithMark(newValueGetter.apply(hitByZenith(entity)), entity.level().getGameTime() + 20)
        );
    }

    public static DamageMark getMark(Entity entity) {
        return entity.getData(ZenithAttachments.DAMAGE_MARK);
    }

    public static ZenithHealthMark getHealth(Entity entity) {
        return entity.getData(ZenithAttachments.ZENITH_HEALTH_MARK);
    }

    public static void setHealth(Entity entity, float value) {
        entity.setData(ZenithAttachments.ZENITH_HEALTH_MARK, new ZenithHealthMark(value, entity.level().getGameTime(),true));
    }
}
