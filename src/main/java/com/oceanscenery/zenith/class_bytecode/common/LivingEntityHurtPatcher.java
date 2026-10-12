package com.oceanscenery.zenith.class_bytecode.common;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class LivingEntityHurtPatcher {
    private static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    private static final String OBJECT = "java/lang/Object";
    private static final String SERVER_LEVEL = "net/minecraft/server/level/ServerLevel";
    private static final String DAMAGE_SOURCE = "net/minecraft/world/damagesource/DamageSource";
    private static final String HURT = "hurtServer";
    private static final String HURT_DESCRIPTOR = String.format(
        "(L%s;L%s;F)Z", SERVER_LEVEL, DAMAGE_SOURCE
    );
    private static final String HURT_HOOK_DESCRIPTOR = String.format(
        "(L%s;FL%s;)F", DAMAGE_SOURCE, LIVING_ENTITY
    );

    private static final String HURT_HOOK_NAME = "beforeHurt";
    private static final String HURT_CHECK_HOOK_NAME = "shouldHurt";
    private static final String HURT_CHECK_HOOK_DESCRIPTOR = String.format(
        "(L%s;L%s;)Z", DAMAGE_SOURCE, LIVING_ENTITY
    );

    private static final String ACTUALLY_HURT_NAME = "actuallyHurt";
    private static final String ACTUALLY_HURT_DESCRIPTOR = String.format(
        "(L%s;L%s;F)V",SERVER_LEVEL,DAMAGE_SOURCE
    );
    private static final String SET_HEALTH_NAME = "setHealth";
    private static final String SET_HEALTH_DESCRIPTOR = "(F)V";

    private static final String ACTUALLY_HURT_HOOK_NAME = "beforeSetHealthInActHurt";
    private static final String ACTUALLY_HURT_HOOK_DESCRIPTOR = String.format(
        "(L%s;FL%s;)V", "java/lang/Object", "java/lang/Object"
    );

    private final LivingEntityHurtPatcher.BytecodeLookup bytecodeLookup;
    private final Map<String, Boolean> hierarchyCache = new ConcurrentHashMap<>();

    @FunctionalInterface
    public interface BytecodeLookup {
        byte[] getClassBytes(String binaryName) throws ClassNotFoundException;
    }

    public LivingEntityHurtPatcher(BytecodeLookup bytecodeLookup) {
        this.bytecodeLookup = Objects.requireNonNull(bytecodeLookup, "bytecodeLookup");
    }

    public boolean transform(ClassNode classNode) {
        boolean isLivingEntity = LIVING_ENTITY.equals(classNode.name);
        if (!isLivingEntity && !extendsLivingEntity(classNode.superName)) {
            return false;
        }

        boolean changed = false;
        for (MethodNode method : classNode.methods) {
            changed |= transformHurt(method);
            changed |= transformActuallyHurt(method);
        }

        return changed;
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

    private boolean transformHurt(MethodNode method) {
        if (!HURT.equals(method.name) || !HURT_DESCRIPTOR.equals(method.desc)) {
            return false;
        }

        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC)) != 0) {
            return false;
        }

        AbstractInsnNode ins = method.instructions.getFirst();
        if (ins == null) {
            return false;
        }
        InsnList patch = new InsnList();

        patch.add(new VarInsnNode(Opcodes.ALOAD, 2));
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
        patch.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            HookNames.OWNER,
            HURT_CHECK_HOOK_NAME,
            HURT_CHECK_HOOK_DESCRIPTOR
        ));
        LabelNode continueNode = new LabelNode();
        patch.add(new JumpInsnNode(Opcodes.IFNE, continueNode));
        patch.add(new InsnNode(Opcodes.ICONST_0));
        patch.add(new InsnNode(Opcodes.IRETURN));

        patch.add(continueNode);
        patch.add(new VarInsnNode(Opcodes.ALOAD, 2));
        patch.add(new VarInsnNode(Opcodes.FLOAD, 3));
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
        patch.add(
            new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HookNames.OWNER,
                HURT_HOOK_NAME,
                HURT_HOOK_DESCRIPTOR
            )
        );
        patch.add(new VarInsnNode(Opcodes.FSTORE, 3));

        method.instructions.insertBefore(ins, patch);
        return true;
    }

    private boolean transformActuallyHurt(MethodNode method) {
        if (!ACTUALLY_HURT_DESCRIPTOR.equals(method.desc)
            || (!ACTUALLY_HURT_NAME.equals(method.name))) {
            return false;
        }

        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC)) != 0) {
            return false;
        }

        List<MethodInsnNode> callSetHealthList = new ArrayList<>();
        for (AbstractInsnNode ins : method.instructions) {
            if (ins instanceof MethodInsnNode mins) {
                if (ins.getOpcode() == Opcodes.INVOKESPECIAL || ins.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    if ((SET_HEALTH_NAME.equals(mins.name)) && SET_HEALTH_DESCRIPTOR.equals(mins.desc)) {
                        callSetHealthList.add(mins);
                    }
                }
            }
        }

        if (callSetHealthList.isEmpty()) {
            return false;
        }
        for (MethodInsnNode mins : callSetHealthList) {
            InsnList patch = new InsnList();
            patch.add(new InsnNode(Opcodes.DUP2));
            patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
            patch.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HookNames.OWNER,
                ACTUALLY_HURT_HOOK_NAME,
                ACTUALLY_HURT_HOOK_DESCRIPTOR,
                false
            ));
            method.instructions.insertBefore(mins, patch);
        }
        return true;
    }
}
