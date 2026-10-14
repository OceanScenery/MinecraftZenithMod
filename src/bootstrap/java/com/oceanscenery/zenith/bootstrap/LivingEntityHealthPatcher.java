package com.oceanscenery.zenith.bootstrap;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Set;

public final class LivingEntityHealthPatcher {
    private static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    private static final String OBJECT = "java/lang/Object";
    private static final String GET_HEALTH_NAME = "getHealth";
    private static final String GET_HEALTH_SRG_NAME = "m_21223_";
    private static final String GET_HEALTH_DESCRIPTOR = "()F";
    private static final String IS_DEAD_OR_DYING_NAME = "isDeadOrDying";
    private static final String IS_DEAD_OR_DYING_SRG_NAME = "m_21224_";
    private static final String IS_DEAD_OR_DYING_DESCRIPTOR = "()Z";
    private static final String IS_ALIVE_NAME = "isAlive";
    private static final String IS_ALIVE_SRG_NAME = "m_6084_";
    private static final String IS_ALIVE_DESCRIPTOR = "()Z";
    private static final String SET_HEALTH_NAME = "setHealth";
    private static final String SET_HEALTH_SRG_NAME = "m_21153_";
    private static final String SET_HEALTH_DESCRIPTOR = "(F)V";

    private final Set<String> targetNames;

    public LivingEntityHealthPatcher(Set<String> targets) {
        this.targetNames = Set.copyOf(targets);
    }

    public boolean transform(ClassNode classNode, String targetName) {
        if (!this.targetNames.contains(targetName)) {
            return false;
        }

        boolean changed = false;
        for (MethodNode method : classNode.methods) {
            if (processGetHealth(method)) {
                changed = true;
            }
            if (processIsAlive(method)) {
                changed = true;
            }
            if (processSetHealth(method)) {
                changed = true;
            }
            if (processIsDeadOrDying(method)) {
                changed = true;
            }
        }

        return changed;
    }

    private boolean processGetHealth(MethodNode method) {
        if (!matches(method, GET_HEALTH_NAME, GET_HEALTH_SRG_NAME, GET_HEALTH_DESCRIPTOR)) {
            return false;
        }

        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC)) != 0) {
            return false;
        }

        boolean flag = false;
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (instruction.getOpcode() != Opcodes.FRETURN) {
                continue;
            }

            InsnList patch = new InsnList();
            patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
            patch.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                ZenithTransformer.CLASS_NAME,
                ZenithTransformer.GET_HEALTH_RETURN,
                ZenithTransformer.GET_HEALTH_RETURN_DES,
                false
            ));

            method.instructions.insertBefore(instruction, patch);
            flag = true;
        }
        return flag;
    }

    private boolean processSetHealth(MethodNode method) {
        if (!matches(method, SET_HEALTH_NAME, SET_HEALTH_SRG_NAME, SET_HEALTH_DESCRIPTOR)) {
            return false;
        }

        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC)) != 0) {
            return false;
        }

        AbstractInsnNode ins = method.instructions.getFirst();
        if (ins == null) {
            return false;
        }

        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.FLOAD, 1));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            ZenithTransformer.CLASS_NAME,
            ZenithTransformer.SET_HEALTH_HEAD,
            ZenithTransformer.SET_HEALTH_HEAD_DES,
            false
        ));

        method.instructions.insertBefore(ins, list);
        return true;
    }

    private boolean processIsDeadOrDying(MethodNode method) {
        if (!matches(method, IS_DEAD_OR_DYING_NAME, IS_DEAD_OR_DYING_SRG_NAME, IS_DEAD_OR_DYING_DESCRIPTOR)) {
            return false;
        }

        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC)) != 0) {
            return false;
        }

        boolean flag = false;
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (instruction.getOpcode() != Opcodes.IRETURN) {
                continue;
            }

            InsnList patch = new InsnList();
            patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
            patch.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                ZenithTransformer.CLASS_NAME,
                ZenithTransformer.IS_DEAD_OR_DYING_RETURN,
                ZenithTransformer.IS_DEAD_OR_DYING_RETURN_DES,
                false
            ));

            method.instructions.insertBefore(instruction, patch);
            flag = true;
        }
        return flag;
    }

    private boolean processIsAlive(MethodNode method) {
        if (!matches(method, IS_ALIVE_NAME, IS_ALIVE_SRG_NAME, IS_ALIVE_DESCRIPTOR)) {
            return false;
        }

        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC)) != 0) {
            return false;
        }

        boolean flag = false;
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (instruction.getOpcode() != Opcodes.IRETURN) {
                continue;
            }

            InsnList patch = new InsnList();
            patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
            patch.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                ZenithTransformer.CLASS_NAME,
                ZenithTransformer.IS_ALIVE_RETURN,
                ZenithTransformer.IS_ALIVE_RETURN_DES,
                false
            ));

            method.instructions.insertBefore(instruction, patch);
            flag = true;
        }
        return flag;
    }

    private static boolean matches(
        MethodNode method,
        String officialName,
        String mappedName,
        String descriptor
    ) {
        return descriptor.equals(method.desc)
            && (officialName.equals(method.name) || mappedName.equals(method.name));
    }
}
