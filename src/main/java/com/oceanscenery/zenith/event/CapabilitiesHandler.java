package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithCapabilities;
import com.oceanscenery.zenith.zenith_class.capabilities.ZenithIdMarkSerializable;
import com.oceanscenery.zenith.zenith_class.capabilities.ZenithUuidMarkSerializable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID)
public class CapabilitiesHandler {
    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event){
        ZenithIdMarkSerializable idMarkProvider=new ZenithIdMarkSerializable();
        ZenithUuidMarkSerializable uuidMarkProvider=new ZenithUuidMarkSerializable();

        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith_id_mark"),idMarkProvider);
        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith_uuid_mark"),uuidMarkProvider);

        event.addListener(
                idMarkProvider.idMarkOptional::invalidate
        );
        event.addListener(
                uuidMarkProvider.optional::invalidate
        );
    }

    public static void toggleIdCap(Entity user,Entity target){
        if(target==null || user==null){
            return;
        }
        String id= EntityType.getKey(target.getType()).toString();
        var unsure=user.getCapability(ZenithCapabilities.ZENITH_ID_MARK);
        if(!unsure.isPresent() || unsure.resolve().isEmpty()){
            return;
        }
        Set<String> idSet=unsure.resolve().get().getIdSet();

        if(idSet.contains(id)){
            if(user instanceof ServerPlayer player){
                player.displayClientMessage(
                        Component.translatable("the_zenith_sword.message.remove_group_entities_to_list").append(":"+id),
                        true
                );
            }
            idSet.remove(id);
        }else{
            if(user instanceof ServerPlayer player){
                player.displayClientMessage(
                        Component.translatable("the_zenith_sword.message.add_group_entities_to_list").append(":"+id),
                        true
                );
            }
            idSet.add(id);
        }
    }

    public static void toggleUuidCap(Entity user,Entity target){
        if(target==null || user==null){
            return;
        }
        UUID id=user.getUUID();
        UUID targetId=target.getUUID();

        var unsure=target.getCapability(ZenithCapabilities.ZENITH_UUID_MARK);
        if(!unsure.isPresent() || unsure.resolve().isEmpty()){
            return;
        }

        Set<UUID> uuidSet=unsure.resolve().get().getUuidSet();

        if(uuidSet.contains(id)){
            if(user instanceof ServerPlayer player){
                player.displayClientMessage(
                        Component.translatable("the_zenith_sword.message.remove_single_entity_to_list").append(":"+targetId),
                        true
                );
            }
            uuidSet.remove(id);
        }else{
            if(user instanceof ServerPlayer player){
                player.displayClientMessage(
                        Component.translatable("the_zenith_sword.message.add_single_entity_to_list").append(":"+targetId),
                        true
                );
            }
            uuidSet.add(id);
        }
    }

    public static boolean checkCanAttack(Entity attacker,Entity victim){
        if(attacker==null || victim==null){
            return true;
        }

        String id=EntityType.getKey(victim.getType()).toString();
        UUID uuid=attacker.getUUID();

        var unsure1=victim.getCapability(ZenithCapabilities.ZENITH_UUID_MARK);
        var unsure2=attacker.getCapability(ZenithCapabilities.ZENITH_ID_MARK);

        if(!unsure1.isPresent() || unsure1.resolve().isEmpty()){
            return true;
        }
        if(!unsure2.isPresent() || unsure2.resolve().isEmpty()){
            return true;
        }

        Set<UUID> onVictim=unsure1.resolve().get().getUuidSet();
        Set<String> onAttacker=unsure2.resolve().get().getIdSet();

        if(onVictim.contains(uuid) || onAttacker.contains(id)){
            return false;
        }
        return true;
    }
}
