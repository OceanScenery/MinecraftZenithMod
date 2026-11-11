package com.oceanscenery.agent.patcher;

import com.oceanscenery.agent.util.MethodConst;
import com.oceanscenery.agent.util.MethodData;
import com.oceanscenery.agent.util.MethodData.ParamType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Set;

public class LivingRemovePatcher {
    public static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    public static final String ENTITY = "net/minecraft/world/entity/Entity";
    public static final String OBJECT = "java/lang/Object";
    public static final String REMOVAL_REASON = "net/minecraft/world/entity/Entity$RemovalReason";
    public static final String HOOKS = "com/oceanscenery/zenith/hooks/Hooks";

    public static final MethodData ON_REMOVE = MethodData.of(
        HOOKS, "onRemoveEnd", "onRemoveEnd",
        ParamType.of(ParamType.ofClass(OBJECT), ParamType.ofClass(OBJECT), ParamType.VOID)
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

        Boolean flag = false;

        if (REMOVE.compareWith(method) || SET_REMOVED.compareWith(method)) {
            for (var ret : method.instructions) {
                if (ret.getOpcode() != Opcodes.RETURN) {
                    continue;
                }

                InsnList insl = new InsnList();
                insl.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insl.add(new VarInsnNode(Opcodes.ALOAD, 1));
                insl.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    ON_REMOVE.owner(),
                    ON_REMOVE.name(),
                    ON_REMOVE.desc()
                ));
                method.instructions.insertBefore(ret, insl);
                flag = true;
            }
        }

        if (MethodConst.DISCARD.compareWith(method)) {
            for (var ret : method.instructions) {
                if (ret.getOpcode() != Opcodes.RETURN) {
                    continue;
                }

                InsnList insl = new InsnList();
                insl.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insl.add(new LdcInsnNode("discarded"));
                insl.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    ON_REMOVE.owner(),
                    ON_REMOVE.name(),
                    ON_REMOVE.desc()
                ));
                method.instructions.insertBefore(ret, insl);
                flag = true;
            }
        }

        if (MethodConst.KILL.compareWith(method)) {
            for (var ret : method.instructions) {
                if (ret.getOpcode() != Opcodes.RETURN) {
                    continue;
                }

                InsnList insl = new InsnList();
                insl.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insl.add(new LdcInsnNode("killed"));
                insl.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    ON_REMOVE.owner(),
                    ON_REMOVE.name(),
                    ON_REMOVE.desc()
                ));
                method.instructions.insertBefore(ret, insl);
                flag = true;
            }
        }

        if (IS_REMOVED.compareWith(method)) {
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
        }
        return flag;
    }
}
