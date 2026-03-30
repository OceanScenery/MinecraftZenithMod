package com.oceanscenery.zenith.server;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID)
public class ZenithNetworkHandler {
    public static final Identifier resource=Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID,"main");
    @SubscribeEvent
    public static void registerPackets(final RegisterPayloadHandlersEvent event){
        final PayloadRegistrar REGISTER=event.registrar(resource.toString());

        REGISTER.playToServer(
                CycleZenithDefaultDistancePacket.TYPE,
                CycleZenithDefaultDistancePacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(()->CycleZenithDefaultDistancePacket.handle(payload,context));
                }
        );

        REGISTER.playToServer(
                CycleAttackModePacket.TYPE,
                CycleAttackModePacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(()->CycleAttackModePacket.handle(payload,context));
                }
        );

        REGISTER.playToServer(
                CycleAttackPlayerPacket.TYPE,
                CycleAttackPlayerPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(()->CycleAttackPlayerPacket.handle(payload,context));
                }
        );

        REGISTER.playToServer(
                ZenithAttackPacket.TYPE,
                ZenithAttackPacket.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(()->ZenithAttackPacket.handle(payload,context));
                }
        );
    }

    public static void sendToServer(CustomPacketPayload packet){
        ClientPacketDistributor.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}
