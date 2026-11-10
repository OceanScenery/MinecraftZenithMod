package com.oceanscenery.zenith.server.packet;

import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ToggleZenithSlotOpenStatePacket(boolean open, int playerId) implements IZenithPacket {
    public ToggleZenithSlotOpenStatePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.open);
        buf.writeInt(this.playerId);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(
            () -> {
                ServerPlayer serverPlayer = context.get().getSender();
                if (serverPlayer.getId() != this.playerId) {
                    context.get().setPacketHandled(true);
                    return;
                }

                IZenithSlot slot = CapabilitiesHandler.getSlot(serverPlayer);
                if (slot != null) {
                    slot.toggle(b -> this.open);

                    ZenithNetworkHandler.syncZenithSlot(serverPlayer);
                }
            }
        );
    }
}
