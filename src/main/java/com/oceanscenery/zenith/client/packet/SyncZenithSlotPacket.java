package com.oceanscenery.zenith.client.packet;

import com.oceanscenery.zenith.client.ZenithClientPacketHandler;
import com.oceanscenery.zenith.server.packet.IZenithPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncZenithSlotPacket(
    boolean valid,
    boolean open,
    double distance,
    int mode,
    UUID playerId
) implements IZenithPacket {
    public SyncZenithSlotPacket(FriendlyByteBuf buf) {
        this(
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readDouble(),
            buf.readInt(),
            buf.readUUID()
        );
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.valid);
        buf.writeBoolean(this.open);
        buf.writeDouble(this.distance);
        buf.writeInt(this.mode);
        buf.writeUUID(this.playerId);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(
            () -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ZenithClientPacketHandler.updateZenithSlot(this)
            )
        );
    }
}
