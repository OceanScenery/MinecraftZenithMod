package com.oceanscenery.zenith.coreagent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

public final class ZenithAgent {
    private static final String CALLBACK = "com.oceanscenery.agent.ZenithAgentCallback";

    private ZenithAgent() {
    }

    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        agentmain(args, instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) throws Exception {
        for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
            if (loaded == null || !CALLBACK.equals(loaded.getName())) {
                continue;
            }
            Method callback = loaded.getDeclaredMethod("agentCall", String.class, Instrumentation.class);
            callback.setAccessible(true);
            callback.invoke(null, args, instrumentation);
            return;
        }
        throw new IllegalStateException("agent loaded failed because of unloaded agent callback: " + CALLBACK);
    }
}
