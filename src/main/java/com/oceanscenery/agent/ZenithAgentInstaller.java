package com.oceanscenery.agent;

import com.oceanscenery.zenith.TheZenithMod;
import sun.misc.Unsafe;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ZenithAgentInstaller {
    private static final String AGENT_RESOURCE = "/zenith_agent.jar";
    private static final String SELF_ATTACH_PROPERTY = "jdk.attach.allowAttachSelf";

    private static final boolean FORCE_USE_LOAD_AGENT = Boolean.getBoolean("zenith.agent.config.force_load_agent_without_touch");

    private static boolean attempted;

    private ZenithAgentInstaller() {
    }

    public static synchronized void install() {
        if (attempted) {
            return;
        }
        attempted = true;
        Path agentFile = null;

        try {
            Path agentJar = extractAgentJar();
            agentFile = agentJar;

            if (FORCE_USE_LOAD_AGENT) {
                throw new RuntimeException("调试选项");
            }

            allowSelfAttach();
            attach(agentJar);

            TheZenithMod.LOGGER.info("[ZenithAgent] process transformer to bytecode");
        } catch (Throwable t1) {
            TheZenithMod.LOGGER.error("[ZenithAgent] failed to registry agent", t1);
            try {
                if (agentFile == null) {
                    TheZenithMod.LOGGER.error("[ZenithAgent] failed to load agent jar file.");
                    return;
                }

                loadAgentWithoutAttach(agentFile);
            } catch (Throwable t2) {
                TheZenithMod.LOGGER.error("[ZenithAgent] in-process loadAgent failed", t2);
                TheZenithMod.LOGGER.error("[ZenithAgent] failed to load agent by all means.");
            }
        }
    }

    private static Path extractAgentJar() throws Exception {
        try (InputStream input = ZenithAgentInstaller.class.getResourceAsStream(AGENT_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("found no inner jar source " + AGENT_RESOURCE + " which may be build error");
            }
            Path target = Files.createTempFile("zenith_agent", ".jar");
            target.toFile().deleteOnExit();
            try (OutputStream output = Files.newOutputStream(target)) {
                input.transferTo(output);
            }
            return target;
        }
    }

    private static void allowSelfAttach() throws Exception {
        if ("true".equalsIgnoreCase(System.getProperty(SELF_ATTACH_PROPERTY))) {
            return;
        }

        Class<?> hotSpotVirtualMachine = Class.forName("sun.tools.attach.HotSpotVirtualMachine");
        Field allowAttachSelf = hotSpotVirtualMachine.getDeclaredField("ALLOW_ATTACH_SELF");

        Unsafe unsafe = getUnsafe();
        var base = unsafe.staticFieldBase(allowAttachSelf);
        long offset = unsafe.staticFieldOffset(allowAttachSelf);
        unsafe.putBoolean(base, offset, true);

        TheZenithMod.LOGGER.info("[ZenithAgent] 已通过 Unsafe 打开 ALLOW_ATTACH_SELF");
    }

    private static void attach(Path agentJar) throws Exception {
        Class<?> virtualMachine = Class.forName("com.sun.tools.attach.VirtualMachine");
        String pid = String.valueOf(ProcessHandle.current().pid());

        TheZenithMod.LOGGER.info("[ZenithAgent] attaching agent to pid {}", pid);
        Object vm = virtualMachine.getMethod("attach", String.class).invoke(null, pid);
        try {
            Method loadAgent = virtualMachine.getMethod("loadAgent", String.class);
            loadAgent.invoke(vm, agentJar.toAbsolutePath().toString());
        } finally {
            virtualMachine.getMethod("detach").invoke(vm);
        }
    }

    private static Unsafe getUnsafe() throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field theUnsafe = unsafeType.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);
        return (Unsafe) unsafe;
    }

    private static MethodHandles.Lookup implLookup() throws Exception {
        Class<?> lookupClass = Class.forName("java.lang.invoke.MethodHandles$Lookup");
        Field f = lookupClass.getDeclaredField("IMPL_LOOKUP");   // 这个字段没有被过滤
        Unsafe unsafe = getUnsafe();
        Object base = unsafe.staticFieldBase(f);
        long off = unsafe.staticFieldOffset(f);
        return (MethodHandles.Lookup) unsafe.getObject(base, off);
    }

    private static void loadAgentWithoutAttach(Path agentJar) throws Throwable {
        String path = agentJar.toAbsolutePath().toString();
        MethodHandles.Lookup lookup = implLookup();
        ClassLoader system = ClassLoader.getSystemClassLoader();
        //挂到系统路径
        MethodHandle append = lookup.findVirtual(
            system.getClass(),
            "appendToClassPathForInstrumentation",
            MethodType.methodType(void.class, String.class)
        );
        append.invoke(system, path);

        MethodHandle loadAgent = lookup.findStatic(
            Class.forName("sun.instrument.InstrumentationImpl"),
            "loadAgent",
            MethodType.methodType(void.class, String.class)
        );
        loadAgent.invoke(path);
    }
}
