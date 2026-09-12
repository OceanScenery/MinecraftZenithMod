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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record ToggleBlacklistPacket(int playerId,int entityId,boolean shifted) implements CustomPacketPayload{
    public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith_blacklist");
    public static final CustomPacketPayload.Type<ToggleBlacklistPacket> TYPE=new CustomPacketPayload.Type<>(ID);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<ByteBuf,ToggleBlacklistPacket> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.INT,ToggleBlacklistPacket::playerId,
            ByteBufCodecs.INT,ToggleBlacklistPacket::entityId,
            ByteBufCodecs.BOOL,ToggleBlacklistPacket::shifted,
            ToggleBlacklistPacket::new
    );

    public static void handle(final ToggleBlacklistPacket packet, final IPayloadContext context){
        if(context.player() instanceof ServerPlayer player && player.level() instanceof ServerLevel serverLevel){
            if(player.getId()!=packet.playerId){
                return;
            }
            Entity selected=serverLevel.getEntity(packet.entityId);
            if(selected==null || selected.distanceTo(player)>player.entityInteractionRange()){
                return;
            }
            if(packet.shifted()){
                AttachmentUtil.toggleIdAttachment(player,selected);
            }else{
                AttachmentUtil.toggleUuidAttachment(player,selected);
            }
            ZenithNetworkHandler.sendToPlayer(
                    player,
                    new ZenithSendPickedEntityInfPacket(
                            selected.getId(),
                            !AttachmentUtil.checkCanAttack(player,selected)
                    )
            );
        }
    }
}
