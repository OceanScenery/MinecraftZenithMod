package com.oceanscenery.agent;

import com.oceanscenery.zenith.TheZenithMod;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ZenithAgentCallback {
    private static final Set<String> PATCHED = ConcurrentHashMap.newKeySet();
    private static final boolean TRANSFORMER_PROBE =
        Boolean.getBoolean("zenith.debug.transformer.probe");
    private static final int CACHE_CLEAR_INTERVAL = 128;
    private static final int CACHE_PRESSURE_CLEAR_INTERVAL = 16;
    private static final double CACHE_CLEAR_HEAP_RATIO = 0.80d;

    private static Instrumentation instrumentation; //参数
    private static ZenithTransformer transformer; //字节码转换器
    private static Set<String> targets = Set.of(); //目标类名集合
    private static final boolean loaded = true;

    private ZenithAgentCallback() {
    }

    public static boolean preload() {
        return loaded;
    }

    public static void agentCall(String args, Instrumentation inst) {
        instrumentation = inst;
        TheZenithMod.LOGGER.info("[Zenith] instrumentation acquired: retransform={} redefine={}",
            inst.isRetransformClassesSupported(), inst.isRedefineClassesSupported());

        try {
            targets = LivingClassFinder.findTargets();
            transformer = new ZenithTransformer(targets);
            TheZenithMod.LOGGER.info("[Zenith] post-mixin patching enabled for {} target classes", targets.size());

            Patcher patcher = new Patcher();
            inst.addTransformer(patcher, true);
            retransformLoaded();
        } catch (Exception exception) {
            TheZenithMod.LOGGER.error("[Zenith] agent callback failed", exception);
            throw new IllegalStateException("Zenith agent callback failed", exception);
        }
    }

    private static void retransformLoaded() {
        List<Class<?>> pending = new ArrayList<>();
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (clazz == null || clazz.isArray() || clazz.isPrimitive()) {
                continue;
            }
            String name = clazz.getName().replace('.', '/');
            if ("net/minecraft/world/entity/Entity".equals(name)) {
                pending.add(clazz);
                continue;
            }
            if (!targets.contains(name) || !instrumentation.isModifiableClass(clazz)) {
                continue;
            }
            pending.add(clazz);
        }
        if (pending.isEmpty()) {
            clearAllAnalysisCaches();
            return;
        }

        long started = System.currentTimeMillis();
        int transformed = 0;
        int failed = 0;
        int sinceCacheClear = 0;
        try {
            List<Class<?>> retransforms = new ArrayList<>();
            for (Class<?> clazz : pending) {
                try {
                    retransforms.add(clazz);
                    if (retransforms.size() >= 10) {
                        instrumentation.retransformClasses(retransforms.toArray(Class[]::new));
                        transformed += retransforms.size();
                        retransforms.clear();
                    }
                } catch (Exception exception) {
                    for (Class<?> c : retransforms) {
                        try {
                            instrumentation.retransformClasses(c);
                            transformed++;
                        } catch (Exception e) {
                            failed++;
                            TheZenithMod.LOGGER.error(
                                "[ZenithAgent] retransform failed for {}, continuing with remaining classes",
                                c.getName(),
                                e
                            );
                        }
                    }
                    retransforms.clear();
                }
                sinceCacheClear++;
                boolean heapPressure = sinceCacheClear >= CACHE_PRESSURE_CLEAR_INTERVAL && heapUsageRatio() >= CACHE_CLEAR_HEAP_RATIO;
                if (sinceCacheClear >= CACHE_CLEAR_INTERVAL || heapPressure) {
                    clearTraceCaches();
                    sinceCacheClear = 0;
                }
                if (heapPressure) {
                    LivingClassFinder.clearCaches();
                }
            }
            // Flush the final batch when the pending count is not a multiple of 10.
            if (!retransforms.isEmpty()) {
                try {
                    instrumentation.retransformClasses(retransforms.toArray(Class[]::new));
                    transformed += retransforms.size();
                } catch (Exception exception) {
                    for (Class<?> c : retransforms) {
                        try {
                            instrumentation.retransformClasses(c);
                            transformed++;
                        } catch (Exception e) {
                            failed++;
                            TheZenithMod.LOGGER.error(
                                "[ZenithAgent] retransform failed for {}, continuing with remaining classes",
                                c.getName(),
                                e
                            );
                        }
                    }
                } finally {
                    retransforms.clear();
                }
            }
        } finally {
            clearAllAnalysisCaches();
        }
        TheZenithMod.LOGGER.info(
            "[ZenithAgent] retransformed {} loaded target classes ({} failed) in {} ms",
            transformed,
            failed,
            System.currentTimeMillis() - started
        );
    }

    private static void clearTraceCaches() {
        if (transformer != null) {
            transformer.clearAnalysisCaches();
        }
    }

    private static void clearAllAnalysisCaches() {
        clearTraceCaches();
        LivingClassFinder.clearCaches();
    }

    private static double heapUsageRatio() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        if (max <= 0L) {
            return 0d;
        }
        return (double) (runtime.totalMemory() - runtime.freeMemory()) / max;
    }

    private static final class Patcher implements ClassFileTransformer {
        @Override
        public byte[] transform(Module module, ClassLoader loader
            , String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classBuffer) {
            if (className == null || classBuffer == null || classBuffer.length == 0) {
                return null;
            }
            if (!targets.contains(className)) {
                return null;
            }

            try {
                ClassNode node = new ClassNode();
                ClassReader reader = new ClassReader(classBuffer);
                reader.accept(node, 0);
                if (TRANSFORMER_PROBE
                    && "net/minecraft/world/entity/LivingEntity".equals(className)) {
                    probeLivingEntityInput(node);
                }
                ClassLoader resourceLoader = classBeingRedefined != null ? classBeingRedefined.getClassLoader() : loader;
                if (!transformer.patch(node, className, resourceLoader)) {
                    return null;
                }

                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                node.accept(writer);
                if (PATCHED.add(className)) {
                    TheZenithMod.LOGGER.info("[ZenithAgent] patched {} (post-mixin)", className);
                }
                return writer.toByteArray();
            } catch (Exception exception) {
                TheZenithMod.LOGGER.error("[ZenithAgent] failed to modify {}", className, exception);
                throw new IllegalStateException("Zenith failed to modify " + className, exception);
            }
        }

        private static void probeLivingEntityInput(ClassNode node) {
            int helperCount = 0;
            int actuallyHurtHelpers = 0;
            int tickDeathHelpers = 0;
            int dieHelpers = 0;
            for (var method : node.methods) {
                if (!method.name.contains("$mixinextras$wrapped$")) {
                    continue;
                }
                helperCount++;
                if (method.name.startsWith("actuallyHurt$mixinextras$wrapped$")
                    || method.name.startsWith("m_6475_$mixinextras$wrapped$")) {
                    actuallyHurtHelpers++;
                }
                if (method.name.startsWith("tickDeath$mixinextras$wrapped$")
                    || method.name.startsWith("m_6153_$mixinextras$wrapped$")) {
                    tickDeathHelpers++;
                }
                if (method.name.startsWith("die$mixinextras$wrapped$")
                    || method.name.startsWith("m_6667_$mixinextras$wrapped$")) {
                    dieHelpers++;
                }
            }
            TheZenithMod.LOGGER.info(
                "[ZenithProbe] transformer input {}: methods={}, mixinExtrasHelpers={}, actuallyHurt={}, tickDeath={}, die={}",
                node.name,
                node.methods.size(),
                helperCount,
                actuallyHurtHelpers,
                tickDeathHelpers,
                dieHelpers
            );
        }
    }

}
