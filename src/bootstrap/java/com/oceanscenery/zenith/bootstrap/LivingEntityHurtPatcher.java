package com.oceanscenery.zenith.bootstrap;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LivingEntityHurtPatcher {
    private static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    private static final String DAMAGE_SOURCE = "net/minecraft/world/damagesource/DamageSource";
    private static final String HURT_NAME = "hurt";
    private static final String HURT_SRG_NAME = "m_6469_";
    private static final String HURT_DESCRIPTOR = String.format(
        "(L%s;F)Z", DAMAGE_SOURCE
    );
    private static final String ACTUALLY_HURT_NAME = "actuallyHurt";
    private static final String ACTUALLY_HURT_SRG_NAME = "m_6475_";
    private static final String ACTUALLY_HURT_DESCRIPTOR = String.format(
        "(L%s;F)V", DAMAGE_SOURCE
    );
    private static final String SET_HEALTH_NAME = "setHealth";
    private static final String SET_HEALTH_SRG_NAME = "m_21153_";
    private static final String SET_HEALTH_DESCRIPTOR = "(F)V";

    private static final String HOOK_OWNER = "com/oceanscenery/zenith/hooks/Hooks";
    private static final String HURT_HOOK_NAME = "beforeHurt";
    private static final String HURT_HOOK_DESCRIPTOR = String.format(
        "(L%s;FL%s;)F", DAMAGE_SOURCE, LIVING_ENTITY
    );
    private static final String ACTUALLY_HURT_HOOK_NAME = "beforeSetHealthInActHurt";
    private static final String ACTUALLY_HURT_HOOK_DESCRIPTOR = String.format(
        "(L%s;FL%s;)V", "java/lang/Object","java/lang/Object"
    );

    private static final String HURT_CHECK_HOOK_NAME = "shouldHurt";
    private static final String HURT_CHECK_HOOK_DESCRIPTOR = String.format(
        "(L%s;L%s;)Z", DAMAGE_SOURCE, LIVING_ENTITY
    );

    private final Set<String> targetNames;

    public LivingEntityHurtPatcher(Set<String> targets) {
        this.targetNames = Set.copyOf(targets);
    }

    public boolean transform(ClassNode classNode, String targetName) {
        if (!this.targetNames.contains(targetName)) {
            return false;
        }

        boolean changed = false;
        for (MethodNode method : classNode.methods) {
            changed |= transformHurt(method);
            changed |= transformActuallyHurt(method);
        }

        return changed;
    }

    private boolean transformHurt(MethodNode method) {
        if (!HURT_DESCRIPTOR.equals(method.desc)
            || (!HURT_NAME.equals(method.name) && !HURT_SRG_NAME.equals(method.name))) {
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

        patch.add(new VarInsnNode(Opcodes.ALOAD, 1));
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
        patch.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            HOOK_OWNER,
            HURT_CHECK_HOOK_NAME,
            HURT_CHECK_HOOK_DESCRIPTOR
        ));
        LabelNode continueNode = new LabelNode();
        patch.add(new JumpInsnNode(Opcodes.IFNE, continueNode));
        patch.add(new InsnNode(Opcodes.ICONST_0));
        patch.add(new InsnNode(Opcodes.IRETURN));

        patch.add(continueNode);
        patch.add(new VarInsnNode(Opcodes.ALOAD, 1));
        patch.add(new VarInsnNode(Opcodes.FLOAD, 2));
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
        patch.add(
            new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                HOOK_OWNER,
                HURT_HOOK_NAME,
                HURT_HOOK_DESCRIPTOR
            )
        );
        patch.add(new VarInsnNode(Opcodes.FSTORE, 2));

        method.instructions.insertBefore(ins, patch);
        return true;
    }

    private boolean transformActuallyHurt(MethodNode method) {
        if (!ACTUALLY_HURT_DESCRIPTOR.equals(method.desc)
            || (!ACTUALLY_HURT_NAME.equals(method.name) && !ACTUALLY_HURT_SRG_NAME.equals(method.name))) {
            return false;
        }

        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC)) != 0) {
            return false;
        }

        List<MethodInsnNode> callSetHealthList = new ArrayList<>();
        for (AbstractInsnNode ins : method.instructions) {
            if (ins instanceof MethodInsnNode mins) {
                if (ins.getOpcode() == Opcodes.INVOKESPECIAL || ins.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    if ((SET_HEALTH_NAME.equals(mins.name) || SET_HEALTH_SRG_NAME.equals(mins.name)) && SET_HEALTH_DESCRIPTOR.equals(mins.desc)) {
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
                HOOK_OWNER,
                ACTUALLY_HURT_HOOK_NAME,
                ACTUALLY_HURT_HOOK_DESCRIPTOR,
                false
            ));
            method.instructions.insertBefore(mins, patch);
        }
        return true;
    }
}
