package com.oceanscenery.zenith.server.packet;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.attachment.ZenithSlot;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.registry.ZenithAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetZenithGhostConfigPacket(int playerId, double distance, AttackMode mode) implements CustomPacketPayload {
    public static final Type<SetZenithGhostConfigPacket> TYPE = new Type<>(TheZenithMod.loc("set_zenith_ghost_config"));
    public static final StreamCodec<ByteBuf, SetZenithGhostConfigPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, SetZenithGhostConfigPacket::playerId,
        ByteBufCodecs.DOUBLE, SetZenithGhostConfigPacket::distance,
        AttackMode.STREAM_CODEC, SetZenithGhostConfigPacket::mode,
        SetZenithGhostConfigPacket::new
    );

    public static void handle(SetZenithGhostConfigPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.getId() == packet.playerId) {
            AttackMode mode = packet.mode;

            ZenithSlot curr = player.getData(ZenithAttachments.ZENITH_SLOT);

            player.setData(
                ZenithAttachments.ZENITH_SLOT,
                new ZenithSlot(curr.valid(), curr.open(), packet.distance, mode)
            );
        }
    }

    @Override
    public Type<SetZenithGhostConfigPacket> type() {
        return TYPE;
    }
}
