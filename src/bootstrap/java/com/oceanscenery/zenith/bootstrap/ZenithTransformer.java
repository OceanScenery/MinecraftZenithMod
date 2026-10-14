package com.oceanscenery.zenith.bootstrap;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.stream.Collectors;

public final class ZenithTransformer implements ITransformer<ClassNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger("TheZenithSwordBootstrap");
    private static final Set<String> PATCHED_CLASSES = ConcurrentHashMap.newKeySet();
    public static final String CLASS_NAME = "com/oceanscenery/zenith/hooks/Hooks";
    public static final String GET_HEALTH_RETURN = "modifyGetHealth";
    public static final String GET_HEALTH_RETURN_DES = "(FLnet/minecraft/world/entity/LivingEntity;)F";
    public static final String IS_DEAD_OR_DYING_RETURN = "modifyIsDeadOrDying";
    public static final String IS_DEAD_OR_DYING_RETURN_DES = "(ZLnet/minecraft/world/entity/LivingEntity;)Z";
    public static final String SET_HEALTH_HEAD = "beforeSetHealth";
    public static final String SET_HEALTH_HEAD_DES = "(FLnet/minecraft/world/entity/LivingEntity;)V";
    public static final String IS_ALIVE_RETURN = "modifyIsAlive";
    public static final String IS_ALIVE_RETURN_DES = "(ZLnet/minecraft/world/entity/LivingEntity;)Z";

    private final Set<String> internalTargets;
    private final Set<ITransformer.Target> targets;
    private final LivingEntityHealthPatcher healthPatcher;
    private final LivingEntityHurtPatcher hurtPatcher;

    public ZenithTransformer(Set<String> internalTargets) {
        this.internalTargets = Set.copyOf(internalTargets);
        this.targets = internalTargets.stream()
            .map(name -> name.replace('/', '.'))
            .map(ITransformer.Target::targetClass)
            .collect(Collectors.toUnmodifiableSet());
        this.healthPatcher = new LivingEntityHealthPatcher(this.internalTargets);
        this.hurtPatcher = new LivingEntityHurtPatcher(this.internalTargets);
    }

    @Override
    public @NotNull ClassNode transform(ClassNode classNode, ITransformerVotingContext context) {
        String contextName = context.getClassName().replace('.', '/');
        String targetName = internalTargets.contains(contextName) ? contextName : classNode.name;
        boolean changed = hurtPatcher.transform(classNode, targetName);
        changed |= healthPatcher.transform(classNode, targetName);
        if (changed && PATCHED_CLASSES.add(classNode.name)) {
            LOGGER.info("[ZenithTransformer] patched {}", classNode.name);
        }
        return classNode;
    }

    @Override
    public @NotNull TransformerVoteResult castVote(
        ITransformerVotingContext context
    ) {
        String internalName = context.getClassName().replace('.', '/');
        return internalTargets.contains(internalName)
            ? TransformerVoteResult.YES
            : TransformerVoteResult.NO;
    }

    @Override
    public @NotNull Set<ITransformer.Target> targets() {
        return targets;
    }

    @Override
    public String[] labels() {
        return new String[]{"the_zenith_sword:living_entity"};
    }
}
