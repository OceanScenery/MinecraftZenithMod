package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class PendingSync {

    public static final long DEFAULT_DROP_TIME = TimeUnit.SECONDS.toNanos(30L);
    public static final long SHORT_DROP_TIME = TimeUnit.MILLISECONDS.toNanos(500L);

    @FunctionalInterface
    public interface Applier {
        boolean tryApply(ClientLevel level);
    }

    private record Entry(Applier applier, long deadline) {
    }

    private static final Map<String, Entry> QUEUE = new LinkedHashMap<>();

    private PendingSync() {
    }

    public static void enqueue(String key, long discardAfter, Applier applier) {
        if (key == null || applier == null) {
            return;
        }
        QUEUE.put(key, new Entry(applier, System.nanoTime() + Math.max(0L, discardAfter)));
    }

    public static void tryFlush() {
        if (QUEUE.isEmpty()) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        List<Map.Entry<String, Entry>> snapshot = new ArrayList<>(QUEUE.entrySet());
        long now = System.nanoTime();

        for (Map.Entry<String, Entry> snapshotEntry : snapshot) {
            String key = snapshotEntry.getKey();
            Entry entry = snapshotEntry.getValue();

            if (now - entry.deadline() > 0L) {
                remove(key, entry);
                TheZenithMod.LOGGER.debug("[zenith] pending sync expired: {}", key);
                continue;
            }

            try {
                if (entry.applier().tryApply(level)) {
                    remove(key, entry);
                }
            } catch (Throwable t) {
                remove(key, entry);
                TheZenithMod.LOGGER.error("[zenith] pending sync failed: {}", key, t);
            }
        }
    }

    private static void remove(String key, Entry entry) {
        QUEUE.remove(key, entry);
    }

    public static void clear() {
        QUEUE.clear();
    }

    public static int size() {
        return QUEUE.size();
    }
}
