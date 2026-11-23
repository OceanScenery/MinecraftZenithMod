package com.oceanscenery.agent;

import com.oceanscenery.agent.patcher.LivingDeathPatcher;
import com.oceanscenery.agent.patcher.LivingHealthPatcher;
import com.oceanscenery.agent.patcher.LivingHurtPatcher;
import com.oceanscenery.agent.patcher.LivingTotalPatcher;
import com.oceanscenery.agent.patcher.LivingLootPatcher;
import com.oceanscenery.agent.util.MethodData;
import com.oceanscenery.agent.util.MethodData.ParamType;
import org.objectweb.asm.tree.ClassNode;
import net.minecraftforge.fml.ModList;

import java.util.HashSet;
import java.util.Set;

public final class ZenithTransformer {
    private static final Set<String> EXCLUDED_MOD_IDS = Set.of("minecraft", "forge", "the_zenith_sword");
    public static final String ENTITY = "net/minecraft/world/entity/Entity";
    public static final String HOOKS = "com/oceanscenery/zenith/hooks/Hooks";
    public static final String DAMAGE_SOURCE = "net/minecraft/world/damagesource/DamageSource";
    public static final String OBJECT = "java/lang/Object";

    public static final String GET_HEALTH_RETURN = "modifyGetHealth";
    public static final String GET_HEALTH_RETURN_DES = "(FLnet/minecraft/world/entity/LivingEntity;)F";
    public static final String IS_DEAD_OR_DYING_RETURN = "modifyIsDeadOrDying";
    public static final String IS_DEAD_OR_DYING_RETURN_DES = "(ZLnet/minecraft/world/entity/LivingEntity;)Z";
    public static final String SET_HEALTH_HEAD = "beforeSetHealth";
    public static final String SET_HEALTH_HEAD_DES = "(FLnet/minecraft/world/entity/LivingEntity;)V";
    public static final String IS_ALIVE_RETURN = "modifyIsAlive";
    public static final String IS_ALIVE_RETURN_DES = "(ZLnet/minecraft/world/entity/LivingEntity;)Z";
    public static final MethodData BEFORE_KNOCKBACK = MethodData.of(
        HOOKS, "shouldIgnoreKnockBack", "shouldIgnoreKnockBack",
        ParamType.of(ParamType.ofClass(DAMAGE_SOURCE), ParamType.ofClass(OBJECT), ParamType.BOOL)
    );

    private final Set<String> internalTargets;
    private final Set<String> modClasses;
    private final LivingHealthPatcher healthPatcher;
    private final LivingHurtPatcher hurtPatcher;
    private final LivingDeathPatcher deathPatcher;

    public ZenithTransformer(Set<String> internalTargets) {
        this.internalTargets = Set.copyOf(internalTargets);
        Set<String> scanned = new HashSet<>();
        for (var file : ModList.get().getModFiles()) {
            var mods = file.getFile().getModInfos();
            if (mods.isEmpty() || mods.stream().anyMatch(mod -> EXCLUDED_MOD_IDS.contains(mod.getModId()))) {
                continue;
            }
            for (var entry : file.getFile().getScanResult().getClasses()) {
                String name = entry.clazz().getInternalName();
                if (!excluded(name) && !vanilla(name)) {
                    scanned.add(name);
                }
            }
        }
        this.modClasses = Set.copyOf(scanned);
        this.healthPatcher = new LivingHealthPatcher(this.internalTargets);
        this.hurtPatcher = new LivingHurtPatcher(this.internalTargets);
        this.deathPatcher = new LivingDeathPatcher(this.internalTargets);
    }

    public Set<String> targets() {
        return this.internalTargets;
    }

    public boolean matches(String internalName) {
        return vanilla(internalName) || modClasses.contains(internalName);
    }

    static boolean vanilla(String name) {
        return name.startsWith("net/minecraft/");
    }

    private static boolean excluded(String name) {
        return name.startsWith("net/minecraftforge/") || name.startsWith("it/unimi/dsi/fastutil/")
            || name.startsWith("com/oceanscenery/zenith/") || name.startsWith("com/oceanscenery/agent/")
            || name.startsWith("cpw/mods/");
    }

    public boolean patch(ClassNode classNode, String targetName, ClassLoader loader) {
        if (!matches(targetName)) {
            return false;
        }
        boolean vanilla = vanilla(targetName);
        boolean entity = internalTargets.contains(targetName) || LivingTotalPatcher.isEntity(classNode, loader);
        if (vanilla && !entity) {
            return false;
        }
        boolean changed = false;
        if (!vanilla && entity) {
            changed |= LivingLootPatcher.transform(classNode);
        }
        if (internalTargets.contains(targetName)) {
            changed |= hurtPatcher.transform(classNode, targetName);
            changed |= healthPatcher.transform(classNode, targetName);
            changed |= deathPatcher.transform(classNode, targetName, loader);
        }
        changed |= LivingTotalPatcher.transform(classNode, loader, vanilla, entity);

        return changed;
    }

    public void clearAnalysisCaches() {
        this.deathPatcher.clearCaches();
    }
}
