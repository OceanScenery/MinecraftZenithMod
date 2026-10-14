package com.oceanscenery.zenith.server;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.client.packet.ZenithSendPickedEntityInfPacket;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.server.packet.SyncZenithMarkPacket;
import com.oceanscenery.zenith.server.packet.ToggleZenithBlacklistPacket;
import com.oceanscenery.zenith.server.packet.ZenithPacket;
import com.oceanscenery.zenith.server.packet.ZenithUpdatePickedEntityQuestPacket;
import com.oceanscenery.zenith.zenith_class.capabilities.IHitByZenithMark;
import com.oceanscenery.zenith.zenith_class.capabilities.IZenithHealthMark;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ZenithNetworkHandler {
    private static final String VERSION = "O_TheZenithMod_2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "main"),
        () -> VERSION,
        VERSION::equals,
        VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
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

        CHANNEL.messageBuilder(
                SyncZenithMarkPacket.class,
                packetId++,
                NetworkDirection.PLAY_TO_CLIENT
            ).encoder(SyncZenithMarkPacket::write).decoder(SyncZenithMarkPacket::new)
            .consumerMainThread(SyncZenithMarkPacket::handle).add();

        TheZenithMod.LOGGER.info("Loading Packet Handler");
    }

    public static void playToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void playToClient(Object packet, ServerPlayer player) {
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToTracker(Entity tracked, Object... packets) {
        for (Object packet : packets) {
            if (packet != null) {
                CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> tracked),
                    packet
                );
            }
        }
    }

    public static void syncMarks(Entity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        sendToTracker(
            entity,
            createHitPacketFrom(entity),
            createHealthPacketFrom(entity)
        );
    }

    public static SyncZenithMarkPacket createHitPacketFrom(Entity entity) {
        IHitByZenithMark hitMark = CapabilitiesHandler.getHitMark(entity);
        if (hitMark == null) {
            return null;
        }
        SyncZenithMarkPacket p1 = new SyncZenithMarkPacket(
            entity.getId(),
            entity.getUUID(),
            0,
            hitMark.gameTime(),
            hitMark.hit(),
            0f
        );
        return p1;
    }

    public static SyncZenithMarkPacket createHealthPacketFrom(Entity entity) {
        IZenithHealthMark healthMark = CapabilitiesHandler.getHealthMark(entity);
        if (healthMark == null) {
            return null;
        }
        return new SyncZenithMarkPacket(
            entity.getId(),
            entity.getUUID(),
            1,
            healthMark.gameTime(),
            healthMark.rawValid(),
            healthMark.value()
        );
    }

    public static void syncHitMark(Entity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        SyncZenithMarkPacket packet = createHitPacketFrom(entity);
        if (packet != null) {
            CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                packet
            );
        }
    }

    public static void syncHealthMark(Entity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        SyncZenithMarkPacket packet = createHealthPacketFrom(entity);
        if (packet != null) {
            CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                packet
            );
        }
    }

    public static void sendSnapshot(ServerPlayer player, Entity entity) {
        SyncZenithMarkPacket hit = createHitPacketFrom(entity);
        SyncZenithMarkPacket health = createHealthPacketFrom(entity);

        if (hit != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), hit);
        }
        if (health != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), health);
        }
    }
}
