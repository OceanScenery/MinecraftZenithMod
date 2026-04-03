package com.oceanscenery.zenith.server;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
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

        TheZenithMod.LOGGER.info("Loading Packet Handler");
    }

    public static void playToServer(Object packet){
        CHANNEL.sendToServer(packet);
    }
}
