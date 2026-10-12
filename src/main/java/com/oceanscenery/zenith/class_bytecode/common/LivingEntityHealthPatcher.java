package com.oceanscenery.zenith.class_bytecode.common;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class LivingEntityHealthPatcher {
    private static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    private static final String OBJECT = "java/lang/Object";
    private static final String GET_HEALTH = "getHealth";
    private static final String GET_HEALTH_DESCRIPTOR = "()F";
    private static final String IS_DEAD_OR_DYING = "isDeadOrDying";
    private static final String IS_DEAD_OR_DYING_DESCRIPTOR = "()Z";
    private static final String IS_ALIVE = "isAlive";
    private static final String IS_ALIVE_DESCRIPTOR = "()Z";
    private static final String SET_HEALTH = "setHealth";
    private static final String SET_HEALTH_DESCRIPTOR = "(F)V";

    private final BytecodeLookup bytecodeLookup;
    private final Map<String, Boolean> hierarchyCache = new ConcurrentHashMap<>();

    public LivingEntityHealthPatcher(BytecodeLookup bytecodeLookup) {
        this.bytecodeLookup = Objects.requireNonNull(bytecodeLookup, "bytecodeLookup");
    }

    public boolean transform(ClassNode classNode) {
        boolean isLivingEntity = LIVING_ENTITY.equals(classNode.name);
        if (!isLivingEntity && !extendsLivingEntity(classNode.superName)) {
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
        if (!GET_HEALTH.equals(method.name) || !GET_HEALTH_DESCRIPTOR.equals(method.desc)) {
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
                HookNames.OWNER,
                HookNames.GET_HEALTH,
                HookNames.GET_HEALTH_DESCRIPTOR,
                false
            ));

            method.instructions.insertBefore(instruction, patch);
            flag = true;
        }
        return flag;
    }

    private boolean processSetHealth(MethodNode method) {
        if (!SET_HEALTH.equals(method.name) || !SET_HEALTH_DESCRIPTOR.equals(method.desc)) {
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
            HookNames.OWNER,
            HookNames.SET_HEALTH,
            HookNames.SET_HEALTH_DESCRIPTOR,
            false
        ));

        method.instructions.insertBefore(ins, list);
        return true;
    }

    private boolean processIsDeadOrDying(MethodNode method) {
        if (!IS_DEAD_OR_DYING.equals(method.name) || !IS_DEAD_OR_DYING_DESCRIPTOR.equals(method.desc)) {
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
                HookNames.OWNER,
                HookNames.IS_DEAD_OR_DYING,
                HookNames.IS_DEAD_OR_DYING_DESCRIPTOR,
                false
            ));

            method.instructions.insertBefore(instruction, patch);
            flag = true;
        }
        return flag;
    }

    private boolean processIsAlive(MethodNode method) {
        if (!IS_ALIVE.equals(method.name) || !IS_ALIVE_DESCRIPTOR.equals(method.desc)) {
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
                HookNames.OWNER,
                HookNames.IS_ALIVE,
                HookNames.IS_ALIVE_DESCRIPTOR,
                false
            ));

            method.instructions.insertBefore(instruction, patch);
            flag = true;
        }
        return flag;
    }

    private boolean extendsLivingEntity(String superName) {
        if (superName == null || OBJECT.equals(superName)) {
            return false;
        }

        return hierarchyCache.computeIfAbsent(superName, name -> {
            String currentClass = name;
            while (currentClass != null && !OBJECT.equals(currentClass)) {
                if (LIVING_ENTITY.equals(currentClass)) {
                    return true;
                }

                try {
                    byte[] bytes = bytecodeLookup.getClassBytes(currentClass.replace('/', '.'));
                    currentClass = new ClassReader(bytes).getSuperName();
                } catch (ClassNotFoundException | RuntimeException exception) {
                    return false;
                }
            }
            return false;
        });
    }

    private static boolean callsSuperGetHealth(MethodNode method, String superName) {
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            if (!(instruction instanceof MethodInsnNode call)) {
                continue;
            }

            if (call.getOpcode() == Opcodes.INVOKESPECIAL
                && call.owner.equals(superName)
                && GET_HEALTH.equals(call.name)
                && GET_HEALTH_DESCRIPTOR.equals(call.desc)) {
                return true;
            }
        }

        return false;
    }

    @FunctionalInterface
    public interface BytecodeLookup {
        byte[] getClassBytes(String binaryName) throws ClassNotFoundException;
    }
}
