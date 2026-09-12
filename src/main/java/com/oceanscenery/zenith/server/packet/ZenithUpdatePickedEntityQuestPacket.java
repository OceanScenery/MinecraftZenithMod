package com.oceanscenery.zenith.server.packet;

import com.oceanscenery.zenith.client.packet.ZenithSendPickedEntityInfPacket;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ZenithUpdatePickedEntityQuestPacket(int playerId,int entityId) implements IZenithPacket{
    public ZenithUpdatePickedEntityQuestPacket(FriendlyByteBuf buf){
        this(buf.readInt(),buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(playerId).writeInt(entityId);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer serverPlayer=context.get().getSender();
        Level level = serverPlayer.serverLevel();

        Entity query = level.getEntity(entityId);
        boolean result = CapabilitiesHandler.checkCanAttack(serverPlayer, query);

        if(query==null || serverPlayer.getId()!=playerId){
            return;
        }

        ZenithSendPickedEntityInfPacket replyPacket = new ZenithSendPickedEntityInfPacket(entityId, !result);
        ZenithNetworkHandler.playToClient(replyPacket,serverPlayer);
        context.get().setPacketHandled(true);
    }
}
