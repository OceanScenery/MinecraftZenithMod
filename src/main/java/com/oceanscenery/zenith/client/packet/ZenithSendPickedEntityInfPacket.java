package com.oceanscenery.zenith.client.packet;

import com.oceanscenery.zenith.client.ZenithClientPacketHandler;
import com.oceanscenery.zenith.server.packet.IZenithPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ZenithSendPickedEntityInfPacket(int entityId,boolean inList) implements IZenithPacket {
    public ZenithSendPickedEntityInfPacket(FriendlyByteBuf buf){
        this(buf.readInt(),buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId).writeBoolean(inList);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        ZenithClientPacketHandler.handleEntityInfPacket(entityId,inList);
    }
}
