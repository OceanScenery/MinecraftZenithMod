package com.oceanscenery.agent;

import com.oceanscenery.agent.patcher.LivingDeathPatcher;
import com.oceanscenery.agent.patcher.LivingHealthPatcher;
import com.oceanscenery.agent.patcher.LivingHurtPatcher;
import com.oceanscenery.agent.patcher.LivingRemovePatcher;
import com.oceanscenery.agent.util.MethodData;
import com.oceanscenery.agent.util.MethodData.ParamType;
import org.objectweb.asm.tree.ClassNode;

import java.util.Set;

public final class ZenithTransformer {
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
    private final LivingHealthPatcher healthPatcher;
    private final LivingHurtPatcher hurtPatcher;
    private final LivingDeathPatcher deathPatcher;
    private final LivingRemovePatcher removePatcher;

    public ZenithTransformer(Set<String> internalTargets) {
        this.internalTargets = Set.copyOf(internalTargets);
        this.healthPatcher = new LivingHealthPatcher(this.internalTargets);
        this.hurtPatcher = new LivingHurtPatcher(this.internalTargets);
        this.deathPatcher = new LivingDeathPatcher(this.internalTargets);
        this.removePatcher = new LivingRemovePatcher(this.internalTargets);
    }

    public Set<String> targets() {
        return this.internalTargets;
    }

    public boolean matches(String internalName) {
        return this.internalTargets.contains(internalName);
    }

    public boolean patch(ClassNode classNode, String targetName, ClassLoader loader) {
        boolean changed = false;
        changed |= hurtPatcher.transform(classNode, targetName);
        changed |= healthPatcher.transform(classNode, targetName);
        changed |= deathPatcher.transform(classNode, targetName, loader);
        changed |= removePatcher.transform(classNode, targetName);

        return changed;
    }

    public void clearAnalysisCaches() {
        this.deathPatcher.clearCaches();
    }
}
