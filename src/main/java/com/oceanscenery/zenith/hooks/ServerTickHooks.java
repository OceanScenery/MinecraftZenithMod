package com.oceanscenery.zenith.hooks;

import com.oceanscenery.agent.ZenithAgentCallback;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.event.ServerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;

import java.util.function.BooleanSupplier;

public final class ServerTickHooks {
    private ServerTickHooks() {
    }

    public static void onTickPre(MinecraftServer server, BooleanSupplier hasTime) {
        var posted = new TickEvent.ServerTickEvent(TickEvent.Phase.START, hasTime, server);
        CapabilitiesHandler.serverTickCheck(posted);
        ServerEvents.onServerTick(posted);
    }

    public static void onTickPost(MinecraftServer server, BooleanSupplier hasTime) {
        ZenithAgentCallback.tick();
    }
}
