package com.oceanscenery.zenith.client.packet;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.client.RenderEntityTip;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ZenithSendPickedEntityInfPacket(int entityId,boolean inList) implements CustomPacketPayload {
    public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith_select_entity_inf");
    public static final CustomPacketPayload.Type<ZenithSendPickedEntityInfPacket> TYPE=new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<ByteBuf,ZenithSendPickedEntityInfPacket> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.INT,ZenithSendPickedEntityInfPacket::entityId,
            ByteBufCodecs.BOOL,ZenithSendPickedEntityInfPacket::inList,
            ZenithSendPickedEntityInfPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ZenithSendPickedEntityInfPacket packet, final IPayloadContext context){
        if(context.player() instanceof LocalPlayer){
            RenderEntityTip.serverBackId =packet.entityId;
            RenderEntityTip.canAttack=!packet.inList;
        }
    }
}
