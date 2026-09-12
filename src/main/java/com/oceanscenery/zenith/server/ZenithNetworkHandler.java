package com.oceanscenery.zenith.server;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.server.packet.ToggleZenithBlacklistPacket;
import com.oceanscenery.zenith.server.packet.ZenithPacket;
import com.oceanscenery.zenith.client.packet.ZenithSendPickedEntityInfPacket;
import com.oceanscenery.zenith.server.packet.ZenithUpdatePickedEntityQuestPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ZenithNetworkHandler {
    private static final String VERSION="O_TheZenithMod";
    public static final SimpleChannel CHANNEL= NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"main"),
            ()->VERSION,
            VERSION::equals,
            VERSION::equals
    );

    private static int packetId=0;

    public static void register(){
        CHANNEL.messageBuilder(
                ZenithPacket.class,
                packetId++,
                NetworkDirection.PLAY_TO_SERVER
        ).encoder(ZenithPacket::write).decoder(ZenithPacket::new)
                .consumerMainThread(ZenithPacket::handle).add();

        CHANNEL.messageBuilder(
                ToggleZenithBlacklistPacket.class,
                packetId++,
                NetworkDirection.PLAY_TO_SERVER
        ).encoder(ToggleZenithBlacklistPacket::write).decoder(ToggleZenithBlacklistPacket::new)
                        .consumerMainThread(ToggleZenithBlacklistPacket::handle).add();

        CHANNEL.messageBuilder(
                ZenithUpdatePickedEntityQuestPacket.class,
                packetId++,
                NetworkDirection.PLAY_TO_SERVER
        ).encoder(ZenithUpdatePickedEntityQuestPacket::write).decoder(ZenithUpdatePickedEntityQuestPacket::new)
                        .consumerMainThread(ZenithUpdatePickedEntityQuestPacket::handle).add();

        CHANNEL.messageBuilder(
                ZenithSendPickedEntityInfPacket.class,
                packetId++,
                NetworkDirection.PLAY_TO_CLIENT
        ).encoder(ZenithSendPickedEntityInfPacket::write).decoder(ZenithSendPickedEntityInfPacket::new)
                        .consumerMainThread(ZenithSendPickedEntityInfPacket::handle).add();

        TheZenithMod.LOGGER.info("Loading Packet Handler");
    }

    public static void playToServer(Object packet){
        CHANNEL.sendToServer(packet);
    }

    public static void playToClient(Object packet, ServerPlayer player){
        CHANNEL.sendTo(packet, player.connection.connection,NetworkDirection.PLAY_TO_CLIENT);
    }
}
