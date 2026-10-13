package com.oceanscenery.zenith.server;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.client.packet.ZenithSendPickedEntityInfPacket;
import com.oceanscenery.zenith.server.packet.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID)
public class ZenithNetworkHandler {
    public static final String resource = TheZenithMod.MOD_ID + "-network";

    @SubscribeEvent
    public static void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar register = event.registrar(resource);

        register.playToServer(
            CycleZenithDefaultDistancePacket.TYPE,
            CycleZenithDefaultDistancePacket.STREAM_CODEC,
            (payload, context)
                -> context.enqueueWork(() -> CycleZenithDefaultDistancePacket.handle(payload, context))
        );

        register.playToServer(
            CycleAttackModePacket.TYPE,
            CycleAttackModePacket.STREAM_CODEC,
            (payload, context)
                -> context.enqueueWork(() -> CycleAttackModePacket.handle(payload, context))
        );

        register.playToServer(
            CycleAttackPlayerPacket.TYPE,
            CycleAttackPlayerPacket.STREAM_CODEC,
            (payload, context)
                -> context.enqueueWork(() -> CycleAttackPlayerPacket.handle(payload, context))
        );

        register.playToServer(
            ZenithAttackPacket.TYPE,
            ZenithAttackPacket.STREAM_CODEC,
            (payload, context)
                -> context.enqueueWork(() -> ZenithAttackPacket.handle(payload, context))
        );

        register.playToServer(
            ToggleBlacklistPacket.TYPE,
            ToggleBlacklistPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(
                () -> ToggleBlacklistPacket.handle(payload, context)
            )
        );

        register.playToServer(
            ZenithUpdatePickedEntityQuestPacket.TYPE,
            ZenithUpdatePickedEntityQuestPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(
                () -> ZenithUpdatePickedEntityQuestPacket.handle(payload, context)
            )
        );

        register.playToClient(
            ZenithSendPickedEntityInfPacket.TYPE,
            ZenithSendPickedEntityInfPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(
                () -> ZenithSendPickedEntityInfPacket.handle(payload, context)
            )
        );
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}
