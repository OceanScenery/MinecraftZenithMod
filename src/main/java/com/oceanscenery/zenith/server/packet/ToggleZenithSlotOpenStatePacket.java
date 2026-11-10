package com.oceanscenery.zenith.server.packet;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.util.AttachmentUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleZenithSlotOpenStatePacket(boolean open, int playerId) implements CustomPacketPayload {
    public static final Type<ToggleZenithSlotOpenStatePacket> TYPE = new Type<>(TheZenithMod.loc("toggle_zenith_slot"));
    public static final StreamCodec<ByteBuf, ToggleZenithSlotOpenStatePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, ToggleZenithSlotOpenStatePacket::open,
        ByteBufCodecs.INT, ToggleZenithSlotOpenStatePacket::playerId,
        ToggleZenithSlotOpenStatePacket::new
    );

    public static void handle(ToggleZenithSlotOpenStatePacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.getId() == packet.playerId) {
            AttachmentUtil.toggleSlotOpen(player, a -> packet.open);
        }
    }

    @Override
    public Type<ToggleZenithSlotOpenStatePacket> type() {
        return TYPE;
    }
}
