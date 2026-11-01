package com.oceanscenery.agent.patcher;

import com.oceanscenery.agent.util.MethodData;
import com.oceanscenery.agent.util.MethodData.ParamType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Set;

public class LivingRemovePatcher {
    public static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    public static final String ENTITY = "net/minecraft/world/entity/Entity";
    public static final String OBJECT = "java/lang/Object";
    public static final String REMOVAL_REASON = "net/minecraft/world/entity/Entity/RemovalReason";
    public static final String HOOKS = "com/oceanscenery/zenith/hooks/Hooks";

    public static final MethodData ON_REMOVE = MethodData.of(
        HOOKS, "onRemoveStart", "onRemoveStart",
        ParamType.of(ParamType.ofClass(OBJECT), ParamType.VOID)
    );
    public static final MethodData ON_IS_REMOVED = MethodData.of(
        HOOKS, "modifyIsRemoved", "modifyIsRemoved",
        ParamType.of(ParamType.BOOL, ParamType.ofClass(OBJECT), ParamType.BOOL)
    );

    public static final MethodData REMOVE = MethodData.of(
        ENTITY, "remove", "m_142687_",
        ParamType.of(
            ParamType.ofClass(REMOVAL_REASON), ParamType.VOID
        )
    );
    public static final MethodData SET_REMOVED = MethodData.of(
        ENTITY, "setRemoved", "m_142467_",
        ParamType.of(
            ParamType.ofClass(REMOVAL_REASON), ParamType.VOID
        )
    );
    public static final MethodData KILL = MethodData.of(
        ENTITY, "kill", "m_6074_",
        ParamType.of(ParamType.VOID)
    );
    public static final MethodData DISCARD = MethodData.of(
        ENTITY, "discard", "m_146870_",
        ParamType.of(ParamType.VOID)
    );
    public static final MethodData IS_REMOVED = MethodData.of(
        ENTITY, "isRemoved", "m_213877_",
        ParamType.of(ParamType.BOOL)
    );

    private final Set<String> targets;

    public LivingRemovePatcher(Set<String> targets) {
        this.targets = Set.copyOf(targets);
    }

    public boolean transform(ClassNode classNode, String targetName) {
        if (!this.targets.contains(targetName)) {
            return false;
        }
        boolean changed = false;
        for (MethodNode method : classNode.methods) {
            changed |= transformMethod(method);
        }
        return changed;
    }

    public boolean transformMethod(MethodNode method) {
        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC)) != 0) {
            return false;
        }

        if (REMOVE.compareWith(method) || SET_REMOVED.compareWith(method)
            || DISCARD.compareWith(method) || KILL.compareWith(method)) {
            if (method.instructions.size() == 0) {
                return false;
            }
            AbstractInsnNode first = method.instructions.getFirst();
            InsnList ins = new InsnList();
            ins.add(new VarInsnNode(Opcodes.ALOAD, 0));
            ins.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                ON_REMOVE.owner(),
                ON_REMOVE.name(),
                ON_REMOVE.desc()
            ));
            method.instructions.insertBefore(first, ins);
            method.maxStack += 1;
            return true;
        }

        if (IS_REMOVED.compareWith(method)) {
            boolean flag = false;
            for (AbstractInsnNode insn : method.instructions) {
                if (insn.getOpcode() == Opcodes.IRETURN) {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        ON_IS_REMOVED.owner(),
                        ON_IS_REMOVED.name(),
                        ON_IS_REMOVED.desc()
                    ));
                    method.instructions.insertBefore(insn, list);
                    flag = true;
                }
            }
            if (flag) {
                method.maxStack += 1;
            }
            return flag;
        }
        return false;
    }
}
