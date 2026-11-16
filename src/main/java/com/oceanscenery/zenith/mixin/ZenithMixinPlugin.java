package com.oceanscenery.zenith.mixin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ZenithMixinPlugin implements IMixinConfigPlugin {
    public ZenithMixinPlugin() {

    }

    private static String innerName(String className) {
        return className.replace('.', '/');
    }

    private static boolean isTickServer(MethodNode node) {
        boolean nameFlag = node.name.equals("tickServer") || node.name.equals("m_5705_");
        boolean descFlag = node.desc.equals("(Ljava/util/function/BooleanSupplier;)V");
        boolean accFlag = (node.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC | Opcodes.ACC_INTERFACE)) == 0;
        return nameFlag && descFlag && accFlag;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        patchTickServer(targetClass);
    }

    private void patchTickServer(ClassNode targetClass) {
        if (targetClass.name.equals(innerName("net.minecraft.server.MinecraftServer"))) {
            for (MethodNode methodNode : targetClass.methods) {
                if (isTickServer(methodNode)) {
                    if (methodNode.instructions.size() != 0) {
                        InsnList atHead = new InsnList();
                        atHead.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        atHead.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        atHead.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            innerName("com.oceanscenery.zenith.hooks.ServerTickHooks"),
                            "onTickPre",
                            "(Lnet/minecraft/server/MinecraftServer;Ljava/util/function/BooleanSupplier;)V",
                            false
                        ));
                        methodNode.instructions.insertBefore(
                            methodNode.instructions.getFirst(),
                            atHead
                        );

                        for (AbstractInsnNode insnNode : methodNode.instructions) {
                            if (insnNode.getOpcode() == Opcodes.RETURN) {
                                InsnList atEnd = new InsnList();
                                atEnd.add(new VarInsnNode(Opcodes.ALOAD, 0));
                                atEnd.add(new VarInsnNode(Opcodes.ALOAD, 1));
                                atEnd.add(new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    innerName("com.oceanscenery.zenith.hooks.ServerTickHooks"),
                                    "onTickPost",
                                    "(Lnet/minecraft/server/MinecraftServer;Ljava/util/function/BooleanSupplier;)V",
                                    false
                                ));
                                methodNode.instructions.insertBefore(
                                    insnNode,
                                    atEnd
                                );
                            }
                        }
                    }
                }
            }
        }
    }
}
