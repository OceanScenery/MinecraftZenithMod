package com.oceanscenery.zenith.server.packet;

import com.oceanscenery.zenith.client.ZenithClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import java.util.UUID;

public record SyncZenithMarkPacket(
    int entityId, UUID entityUuid, int type, long gameTime, boolean flag, float value) implements IZenithPacket {
    public SyncZenithMarkPacket(FriendlyByteBuf buf) {
        this(
            buf.readInt(),
            buf.readUUID(),
            buf.readInt(),
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
        buf.writeLong(gameTime);
        buf.writeBoolean(flag);
        buf.writeFloat(value);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        ZenithClientPacketHandler.handleZenithMarkPacket(this);
        context.get().setPacketHandled(true);
    }
}
