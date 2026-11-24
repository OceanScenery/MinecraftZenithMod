package com.oceanscenery.agent;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ZenithAgentCallback {
    public static final String LATE_AGENT_ARGS = "late";

    private static final int BATCH_SIZE = 8;
    private static final int MAX_ATTEMPTS = 3;

    private static final boolean TRANSFORMER_PROBE =
        Boolean.getBoolean("zenith.debug.transformer.probe");
    private static final boolean DEBUG = Boolean.getBoolean("zenith.debug.transformer.patch");
    private static final int CACHE_CLEAR_INTERVAL = 128;
    private static final int CACHE_PRESSURE_CLEAR_INTERVAL = 16;
    private static final double CACHE_CLEAR_HEAP_RATIO = 0.80d;
    private static final boolean loaded = true;

    private enum State {
        IDLE, EARLY, UPGRADING, LATE, FAILED
    }

    private static final Object LOCK = new Object(); //线程锁
    private static final Patcher ACTIVE_PATCHER = new Patcher();
    private static final Set<String> PATCHED = ConcurrentHashMap.newKeySet();

    private static final Set<Class<?>> PENDING = ConcurrentHashMap.newKeySet();
    private static final Set<Class<?>> RETRANSFORMED = ConcurrentHashMap.newKeySet();
    private static final Map<Class<?>, Integer> ATTEMPTS = new ConcurrentHashMap<>();

    private static volatile State state = State.IDLE;
    private static volatile Instrumentation instrumentation;
    private static volatile Set<String> targets = Set.of();
    private static ZenithTransformer transformer;

    private ZenithAgentCallback() {
    }

    public static boolean preload() {
        return loaded;
    }

    public static void agentCall(String args, Instrumentation inst) {
        if (LATE_AGENT_ARGS.equals(args)) {
            installLateTransformer(inst);
        } else {
            installEarlyTransformer(inst);
        }
    }

    private static void installEarlyTransformer(Instrumentation inst) {
        synchronized (LOCK) {
            if (state != State.IDLE) {
                return;
            }
            instrumentation = inst;
            targets = LivingClassFinder.findTargets();
            transformer = new ZenithTransformer(targets);
            inst.addTransformer(ACTIVE_PATCHER, true);
            TheZenithMod.LOGGER.info("[Zenith] instrumentation acquired: retransform={} redefine={}",
                inst.isRetransformClassesSupported(), inst.isRedefineClassesSupported());
            TheZenithMod.LOGGER.info("[Zenith] post-mixin patching enabled for {} target classes", targets.size());
        }
        retransformLoaded();
        synchronized (LOCK) {
            if (state == State.IDLE) {
                state = State.EARLY;
            }
        }
    }

    private static void installLateTransformer(Instrumentation inst) {
        synchronized (LOCK) {
            if (state != State.UPGRADING) {
                return;
            }
            Instrumentation previous = instrumentation;
            if (previous != null) {
                previous.removeTransformer(ACTIVE_PATCHER);
            }
            inst.addTransformer(ACTIVE_PATCHER, true);
            instrumentation = inst;
            state = State.LATE;
        }
        TheZenithMod.LOGGER.info("[ZenithAgent] transformer moved to the last attached agent");
    }

    private static void requestUpgrade() {
        synchronized (LOCK) {
            if (state != State.EARLY) {
                return;
            }
            state = State.UPGRADING;
        }

        ZenithAgentInstaller.installLateAgent();

        synchronized (LOCK) {
            if (state == State.UPGRADING) {
                state = State.FAILED;
                PENDING.clear();
                TheZenithMod.LOGGER.error(
                    "[ZenithAgent] failed to move the transformer to the last agent, keeping the early one");
            }
        }
    }

    public static void onMismatch(Class<?> entityClass) {
        if (entityClass == null) {
            return;
        }
        if (state == State.EARLY) {
            requestUpgrade();
        }
        if (state == State.LATE) {
            PENDING.add(entityClass);
        }
    }

    public static void tick() {
        if (state != State.LATE || PENDING.isEmpty()) {
            return;
        }
        Instrumentation inst = instrumentation;
        if (inst == null) {
            return;
        }

        Set<Class<?>> batch = new LinkedHashSet<>();
        List<Class<?>> entities = new ArrayList<>();
        Iterator<Class<?>> iterator = PENDING.iterator();
        while (iterator.hasNext() && batch.size() < BATCH_SIZE) {
            Class<?> entityClass = iterator.next();
            iterator.remove();
            entities.add(entityClass);
            collectSuper(inst, entityClass, batch);
        }
        if (batch.isEmpty()) {
            return;
        }
        if (!retransform(inst, batch)) {
            for (Class<?> entityClass : entities) {
                retryLater(entityClass);
            }
        }
    }

    private static void collectSuper(Instrumentation inst, Class<?> entityClass, Set<Class<?>> batch) {
        Set<String> targetNames = targets;
        for (Class<?> current = entityClass;
             current != null && LivingEntity.class.isAssignableFrom(current);
             current = current.getSuperclass()) {
            if (batch.size() >= BATCH_SIZE) {
                return;
            }
            String name = current.getName().replace('.', '/');
            if (targetNames.contains(name)
                && !RETRANSFORMED.contains(current)
                && inst.isModifiableClass(current)) {
                batch.add(current);
            }
            if (current == LivingEntity.class) {
                return;
            }
        }
    }

    private static boolean retransform(Instrumentation inst, Set<Class<?>> batch) {
        try {
            inst.retransformClasses(batch.toArray(Class[]::new));
            RETRANSFORMED.addAll(batch);
            TheZenithMod.LOGGER.info(
                "[ZenithAgent] reordered {} classes with the late transformer", batch.size());
            return true;
        } catch (Throwable throwable) {
            TheZenithMod.LOGGER.warn(
                "[ZenithAgent] batch retransform failed, retrying one by one", throwable);
        }

        boolean complete = true;
        for (Class<?> clazz : batch) {
            try {
                inst.retransformClasses(clazz);
                RETRANSFORMED.add(clazz);
            } catch (Throwable failure) {
                complete = false;
                TheZenithMod.LOGGER.error("[ZenithAgent] failed to retransform {}", clazz.getName(), failure);
            }
        }
        return complete;
    }

    private static void retryLater(Class<?> entityClass) {
        int attempts = ATTEMPTS.merge(entityClass, 1, Integer::sum);
        if (attempts < MAX_ATTEMPTS) {
            PENDING.add(entityClass);
        } else {
            TheZenithMod.LOGGER.error(
                "[ZenithAgent] give up reordering classes of {}", entityClass.getName());
        }
    }

    private static void retransformLoaded() {
        Instrumentation inst = instrumentation;
        if (inst == null) {
            return;
        }

        List<Class<?>> pending = new ArrayList<>();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (clazz == null || clazz.isArray() || clazz.isPrimitive()) {
                continue;
            }
            String name = clazz.getName().replace('.', '/');
            if (!transformer.matches(name, clazz.getModule(), clazz.getClassLoader()) || !inst.isModifiableClass(clazz)
                || (ZenithTransformer.vanilla(name) && !net.minecraft.world.entity.Entity.class.isAssignableFrom(clazz))) {
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
                        inst.retransformClasses(retransforms.toArray(Class[]::new));
                        transformed += 10;
                        retransforms.clear();
                    }
                } catch (Exception | LinkageError exception) {
                    for (Class<?> c : retransforms) {
                        try {
                            inst.retransformClasses(c);
                            transformed++;
                        } catch (Exception | LinkageError e) {
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
            if (!retransforms.isEmpty()) {
                try {
                    inst.retransformClasses(retransforms.toArray(Class[]::new));
                    transformed += retransforms.size();
                } catch (Exception | LinkageError exception) {
                    for (Class<?> c : retransforms) {
                        try {
                            inst.retransformClasses(c);
                            transformed++;
                        } catch (Exception | LinkageError e) {
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

    public static byte[] writePatchedClass(ClassReader reader, ClassNode node) {
        //移除java6以下遗留类的栈信息
        if ((node.version & 0xffff) < Opcodes.V1_6) {
            for (var method : node.methods) {
                var instructions = method.instructions.iterator();
                while (instructions.hasNext()) {
                    if (instructions.next() instanceof FrameNode) {
                        instructions.remove();
                    }
                }
            }
        }
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static final class Patcher implements ClassFileTransformer {
        @Override
        public byte[] transform(Module module, ClassLoader loader
            , String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classBuffer) {
            if (className == null || classBuffer == null || classBuffer.length == 0) {
                return null;
            }
            if (!transformer.matches(className, module, loader)) {
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

                byte[] bytes = writePatchedClass(reader, node);
                if (DEBUG && PATCHED.add(className)) {
                    TheZenithMod.LOGGER.info("[ZenithAgent] patched {} (post-mixin)", className);
                }
                return bytes;
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
