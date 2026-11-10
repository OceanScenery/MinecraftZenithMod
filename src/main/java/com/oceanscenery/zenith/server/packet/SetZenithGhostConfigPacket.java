package com.oceanscenery.zenith.server.packet;

import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SetZenithGhostConfigPacket(int playerId, double distance,
                                         int mode) implements IZenithPacket {
    public SetZenithGhostConfigPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readDouble(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.playerId);
        buf.writeDouble(this.distance);
        buf.writeInt(this.mode);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null || player.getId() != this.playerId) {
                return;
            }

            IZenithSlot slot = CapabilitiesHandler.getSlot(player);
            if (slot == null) {
                return;
            }

            slot.setDistance(this.distance);
            slot.setMode(this.mode);
            ZenithNetworkHandler.sendZenithSlotTo(player, player);
        });
        context.get().setPacketHandled(true);
    }
}
