package com.oceanscenery.zenith.hooks;

import com.oceanscenery.agent.ZenithAgentCallback;
import com.oceanscenery.zenith.event.ServerEvents;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.function.BooleanSupplier;

public final class ServerTickHooks {
    private ServerTickHooks() {
    }

    public static void onTickPre(MinecraftServer server, BooleanSupplier hasTime) {
        var posted = new ServerTickEvent.Pre(hasTime, server);
        ServerEvents.onServerTick(posted);
    }

    public static void onTickPost(MinecraftServer server, BooleanSupplier hasTime) {
        ZenithAgentCallback.tick();
    }
}
