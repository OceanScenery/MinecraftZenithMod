package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.registry.ZenithAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Set;
import java.util.UUID;

public class AttachmentUtil {
    public static void toggleIdAttachment(Entity user, Entity target){
        if(target==null || user ==null){
            return;
        }
        String id= EntityType.getKey(target.getType()).toString();
        Set<String> idSet= user.getData(ZenithAttachments.ZENITH_ID_MARK);

        if(idSet.contains(id)){
            if(user instanceof ServerPlayer player){
                player.sendSystemMessage(
                        Component.translatable("the_zenith_sword.message.remove_group_entities_to_list").append(":"+id),
                        true
                );
            }
            idSet.remove(id);
            user.setData(ZenithAttachments.ZENITH_ID_MARK,idSet);
        }else{
            if(user instanceof ServerPlayer player){
                player.sendSystemMessage(
                        Component.translatable("the_zenith_sword.message.add_group_entities_to_list").append(":"+id),
                        true
                );
            }
            idSet.add(id);
            user.setData(ZenithAttachments.ZENITH_ID_MARK,idSet);
        }
    }

    public static void toggleUuidAttachment(Entity user, Entity target){
        if(target==null || user==null){
            return;
        }

        UUID id=user.getUUID();
        UUID targetId=target.getUUID();
        Set<UUID> uuidSet=target.getData(ZenithAttachments.ZENITH_PLAYER_MARK);

        if(uuidSet.contains(id)){
            if(user instanceof ServerPlayer player){
                player.sendSystemMessage(
                        Component.translatable("the_zenith_sword.message.remove_single_entity_to_list").append(":"+targetId),
                        true
                );
            }
            uuidSet.remove(id);
            target.setData(ZenithAttachments.ZENITH_PLAYER_MARK,uuidSet);
        }else{
            if(user instanceof ServerPlayer player){
                player.sendSystemMessage(
                        Component.translatable("the_zenith_sword.message.add_single_entity_to_list").append(":"+targetId),
                        true
                );
            }
            uuidSet.add(id);
            target.setData(ZenithAttachments.ZENITH_PLAYER_MARK,uuidSet);
        }
    }

    public static boolean checkCanAttack(Entity attacker,Entity victim){
        if(attacker==null || victim==null){
            return true;
        }

        String id=EntityType.getKey(victim.getType()).toString();
        UUID uuid=attacker.getUUID();

        Set<UUID> onVictim=victim.getData(ZenithAttachments.ZENITH_PLAYER_MARK);
        Set<String> onAttacker=attacker.getData(ZenithAttachments.ZENITH_ID_MARK);

        if(onVictim.contains(uuid) || onAttacker.contains(id)){
            return false;
        }
        return true;
    }
}
