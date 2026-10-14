package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.server.packet.SyncZenithMarkPacket;
import com.oceanscenery.zenith.zenith_class.capabilities.IHitByZenithMark;
import com.oceanscenery.zenith.zenith_class.capabilities.IZenithHealthMark;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID, value = Dist.CLIENT)
public final class ZenithClientPacketHandler {
    private static final long MAX_PENDING_AGE_NANOS = 30_000_000_000L;
    private static final Map<UUID, PendingMarks> PENDING = new HashMap<>();

    private ZenithClientPacketHandler() {
    }

    public static void handleEntityInfPacket(int entityId, boolean inList) {
        RenderEntityTip.serverBackId = entityId;
        RenderEntityTip.canAttack = !inList;
    }

    public static void handleZenithMarkPacket(SyncZenithMarkPacket syncZenithMarkPacket) {
        if (syncZenithMarkPacket.type() != 0 && syncZenithMarkPacket.type() != 1) {
            return;
        }

        UUID uuid = syncZenithMarkPacket.entityUuid();
        PendingMarks pending = PENDING.computeIfAbsent(
            uuid,
            ignored -> new PendingMarks(syncZenithMarkPacket.entityId())
        );
        pending.merge(syncZenithMarkPacket, System.nanoTime());

        if (applyPending(uuid, pending)) {
            PENDING.remove(uuid, pending);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }

        UUID uuid = event.getEntity().getUUID();
        PendingMarks pending = PENDING.get(uuid);
        if (pending != null && applyPending(uuid, pending, event.getEntity())) {
            PENDING.remove(uuid, pending);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        long now = System.nanoTime();
        ClientLevel level = Minecraft.getInstance().level;
        Iterator<Map.Entry<UUID, PendingMarks>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingMarks> entry = iterator.next();
            PendingMarks pending = entry.getValue();

            if (level != null) {
                Entity entity = level.getEntity(pending.entityId);
                if (entity != null && applyPending(entry.getKey(), pending, entity)) {
                    iterator.remove();
                    continue;
                }
            }

            if (now - pending.receivedNanos > MAX_PENDING_AGE_NANOS) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PENDING.clear();
    }

    private static boolean applyPending(UUID uuid, PendingMarks pending) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }

        Entity entity = level.getEntity(pending.entityId);
        return entity != null && applyPending(uuid, pending, entity);
    }

    private static boolean applyPending(UUID uuid, PendingMarks pending, Entity entity) {
        if (!entity.getUUID().equals(uuid)) {
            return false;
        }

        if (pending.hit != null && apply(entity, pending.hit)) {
            pending.hit = null;
        }
        if (pending.health != null && apply(entity, pending.health)) {
            pending.health = null;
        }

        return pending.hit == null && pending.health == null;
    }

    private static boolean apply(Entity entity, SyncZenithMarkPacket packet) {
        if (packet.type() == 0) {
            IHitByZenithMark cap = CapabilitiesHandler.getHitMark(entity);
            if (cap == null) {
                return false;
            }
            if (packet.gameTime() < cap.gameTime()) {
                return true;
            }
            cap.set(packet.gameTime(), packet.flag());
            return true;
        }

        IZenithHealthMark mark = CapabilitiesHandler.getHealthMark(entity);
        if (mark == null) {
            return false;
        }
        if (packet.gameTime() < mark.gameTime()) {
            return true;
        }
        mark.set(packet.value(), packet.gameTime(), packet.flag());
        return true;
    }

    private static final class PendingMarks {
        private int entityId;
        private SyncZenithMarkPacket hit;
        private SyncZenithMarkPacket health;
        private long receivedNanos;

        private PendingMarks(int entityId) {
            this.entityId = entityId;
        }

        private void merge(SyncZenithMarkPacket packet, long now) {
            this.entityId = packet.entityId();
            if (packet.type() == 0) {
                this.hit = packet;
            } else if (packet.type() == 1) {
                this.health = packet;
            }
            this.receivedNanos = now;
        }
    }
}
