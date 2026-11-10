package com.oceanscenery.zenith.client.packet;

import com.oceanscenery.zenith.client.ZenithClientPacketHandler;
import com.oceanscenery.zenith.server.packet.IZenithPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncZenithMarkPacket(
    int entityId, UUID entityUuid, int type, long startTime, long endTime, boolean flag,
    float value) implements IZenithPacket {
    public SyncZenithMarkPacket(FriendlyByteBuf buf) {
        this(
            buf.readInt(),
            buf.readUUID(),
            buf.readInt(),
            buf.readLong(),
            buf.readLong(),
            buf.readBoolean(),
            buf.readFloat()
        );
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUUID(entityUuid);
        buf.writeInt(type);
        buf.writeLong(startTime);
        buf.writeLong(endTime);
        buf.writeBoolean(flag);
        buf.writeFloat(value);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        ZenithClientPacketHandler.handleZenithMarkPacket(this);
        context.get().setPacketHandled(true);
    }
}
