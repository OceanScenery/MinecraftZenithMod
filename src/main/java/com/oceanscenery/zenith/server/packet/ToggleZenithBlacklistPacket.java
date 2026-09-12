package com.oceanscenery.zenith.server.packet;

import com.oceanscenery.zenith.client.packet.ZenithSendPickedEntityInfPacket;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ToggleZenithBlacklistPacket(int playerId, int entityId, boolean shifted) implements IZenithPacket {
    public ToggleZenithBlacklistPacket(FriendlyByteBuf buf){
        this(buf.readInt(),buf.readInt(),buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf){
        buf.writeInt(playerId);
        buf.writeInt(entityId);
        buf.writeBoolean(shifted);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player=context.get().getSender();
        if(player.getId()!=playerId){
            context.get().setPacketHandled(true);
            return;
        }
        Entity selected=player.serverLevel().getEntity(entityId);
        if(selected==null || player.distanceTo(selected)>player.getAttributeValue(ForgeMod.ENTITY_REACH.get())){
            context.get().setPacketHandled(true);
            return;
        }
        if(shifted()){
            CapabilitiesHandler.toggleIdCap(player,selected);
        }else{
            CapabilitiesHandler.toggleUuidCap(player,selected);
        }

        ZenithNetworkHandler.playToClient(
                new ZenithSendPickedEntityInfPacket(
                        selected.getId(),
                        !CapabilitiesHandler.checkCanAttack(player,selected)
                ),
                player
        );
    }
}
