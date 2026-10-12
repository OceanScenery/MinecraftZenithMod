package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.util.DamageHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID)
public class ServerTicker {
    public static Queue<ServerTickTask> taskList = new ArrayDeque<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        Iterator<ServerTickTask> iter = taskList.iterator();
        while (iter.hasNext()) {
            ServerTickTask task = iter.next();
            task.tick();
            if (task.isCompleted()) {
                iter.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onStop(ServerStoppedEvent event) {
        taskList.clear();
        DamageHandler.checkedVictims.clear();
    }
}
