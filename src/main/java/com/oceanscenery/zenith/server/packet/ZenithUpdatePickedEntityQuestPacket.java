package com.oceanscenery.zenith.server.packet;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.client.packet.ZenithSendPickedEntityInfPacket;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import com.oceanscenery.zenith.util.AttachmentUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ZenithUpdatePickedEntityQuestPacket(int playerId,int entityId) implements CustomPacketPayload{
    public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith_select_entity_update");
    public static final CustomPacketPayload.Type<ZenithUpdatePickedEntityQuestPacket> TYPE=new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<ByteBuf,ZenithUpdatePickedEntityQuestPacket> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.INT,ZenithUpdatePickedEntityQuestPacket::playerId,
            ByteBufCodecs.INT,ZenithUpdatePickedEntityQuestPacket::entityId,
            ZenithUpdatePickedEntityQuestPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final ZenithUpdatePickedEntityQuestPacket packet, final IPayloadContext context){
        if(context.player() instanceof ServerPlayer serverPlayer){

            Level level = serverPlayer.serverLevel();

            Entity query = level.getEntity(packet.entityId);
            boolean result = AttachmentUtil.checkCanAttack(serverPlayer, query);

            if(query==null || serverPlayer.getId()!=packet.playerId){
                return;
            }

            ZenithSendPickedEntityInfPacket replyPacket = new ZenithSendPickedEntityInfPacket(packet.entityId, !result);
            ZenithNetworkHandler.sendToPlayer(serverPlayer,replyPacket);
        }
    }
}
