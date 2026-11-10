package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.client.packet.SyncZenithMarkPacket;
import com.oceanscenery.zenith.client.packet.SyncZenithSlotPacket;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithHitMark;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithHealthMark;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID, value = Dist.CLIENT)
public final class ZenithClientPacketHandler {

    private static final String KEY_SLOT = "slot:";
    private static final String KEY_MARK_HIT = "mark:hit:";
    private static final String KEY_MARK_HEALTH = "mark:health:";
    private static final String KEY_PICKED = "picked:";

    private ZenithClientPacketHandler() {
    }

    public static void updateZenithSlot(SyncZenithSlotPacket packet) {
        if (tryApplySlot(packet)) {
            return;
        }
        PendingSync.enqueue(
            KEY_SLOT + packet.playerId(),
            PendingSync.DEFAULT_DROP_TIME,
            level -> tryApplySlot(packet)
        );
    }

    public static void handleZenithMarkPacket(SyncZenithMarkPacket packet) {
        if (packet.type() != 0 && packet.type() != 1) {
            return;
        }

        String key = (packet.type() == 0 ? KEY_MARK_HIT : KEY_MARK_HEALTH) + packet.entityUuid();
        if (tryApplyMark(packet)) {
            return;
        }
        PendingSync.enqueue(key, PendingSync.DEFAULT_DROP_TIME, level -> tryApplyMark(packet));
    }

    public static void handleEntityInfPacket(int entityId, boolean inList) {
        if (tryApplyPicked(entityId, inList)) {
            return;
        }
        PendingSync.enqueue(
            KEY_PICKED + entityId,
            PendingSync.SHORT_DROP_TIME,
            level -> tryApplyPicked(entityId, inList)
        );
    }

    private static boolean tryApplySlot(SyncZenithSlotPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }

        Player player = level.getPlayerByUUID(packet.playerId());
        if (player == null) {
            return false;
        }

        IZenithSlot slot = CapabilitiesHandler.getSlot(player);
        if (slot == null) {
            return false;
        }

        slot.setDistance(packet.distance());
        slot.setMode(packet.mode());
        // 先置 valid 再设置 open：IZenithSlot#toggle 内部要求 valid 为 true 才会生效
        slot.setValid(packet.valid());
        slot.toggle(current -> packet.open());
        return true;
    }

    @SubscribeEvent
    public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        Player oldPlayer = event.getOldPlayer();
        Player newPlayer = event.getNewPlayer();
        if (oldPlayer == null || newPlayer == null) {
            return;
        }

        oldPlayer.reviveCaps();
        IZenithSlot from = CapabilitiesHandler.getSlot(oldPlayer);
        IZenithSlot to = CapabilitiesHandler.getSlot(newPlayer);
        if (from == null || to == null) {
            return;
        }

        to.setDistance(from.getDistance());
        to.setMode(from.getMode());
        to.setValid(from.valid());
        to.toggle(current -> from.open());
    }

    private static boolean tryApplyMark(SyncZenithMarkPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }

        Entity entity = level.getEntity(packet.entityId());
        if (entity == null || !entity.getUUID().equals(packet.entityUuid())) {
            return false;
        }

        return apply(entity, packet);
    }

    private static boolean tryApplyPicked(int entityId, boolean inList) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }

        if (entityId < 0 || level.getEntity(entityId) == null) {
            return false;
        }

        RenderEntityTip.serverBackId = entityId;
        RenderEntityTip.canAttack = !inList;
        return true;
    }

    private static boolean apply(Entity entity, SyncZenithMarkPacket packet) {
        if (packet.type() == 0) {
            IZenithHitMark cap = CapabilitiesHandler.getHitMark(entity);
            if (cap == null) {
                return false;
            }
            if (packet.startTime() < cap.startTime()) {
                return true;
            }
            cap.set(packet.startTime(), packet.endTime(), packet.flag());
            return true;
        }

        IZenithHealthMark mark = CapabilitiesHandler.getHealth(entity);
        if (mark == null) {
            return false;
        }
        mark.set(packet.value(), packet.flag());
        return true;
    }

    /**
     * 每刻末尾尝试处理未成功受理的网络包
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        PendingSync.tryFlush();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        PendingSync.tryFlush();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PendingSync.clear();
    }
}
