package com.oceanscenery.agent;

import com.oceanscenery.agent.util.MethodConst;
import com.oceanscenery.agent.util.MethodControlFlow;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * 用于存储一个类的特征信息
 */
public final class ClassInfo {
    private static final String REMOVAL_REASON_FIELD = "removalReason";
    private static final String REMOVAL_REASON_FIELD_SRG = "f_146795_";
    private static final String REMOVAL_REASON_DESC = "L" + MethodConst.REMOVAL_REASON + ";";

    //一条调用 包含调用的类名，方法名，描述符，操作码
    record CallSite(String owner, String name, String desc, int opcode) {
    }

    //一个方法的信息 包含所有方法调用，是否为直接收尾方法，以及是否确定
    record MethodInfo(List<CallSite> calls, boolean directTerminal, boolean certain) {
    }

    //类名
    public final String name;
    //父类类名
    public final String superName;
    //所有的方法的信息
    public final Map<String, MethodInfo> methods;

    private ClassInfo(String name, String superName, Map<String, MethodInfo> methods) {
        this.name = name;
        this.superName = superName;
        this.methods = Map.copyOf(methods);
    }

    //通过方法名和描述生成唯一key
    public static String key(String name, String desc) {
        return name + desc;
    }

    //通过key获取对应的方法信息
    public MethodInfo method(String name, String desc) {
        return this.methods.get(key(name, desc));
    }

    //通过字节码获取类的信息
    public static ClassInfo parse(byte[] bytes) {
        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(bytes);
        reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return fromClassNode(node);
    }

    //不通过字节码，直接从类节点获取信息
    public static ClassInfo fromClassNode(ClassNode node) {
        Map<String, MethodInfo> methods = new LinkedHashMap<>();
        for (MethodNode method : node.methods) {
            if (method.name.startsWith("<")) {
                continue;
            }

            //分析方法控制流
            MethodControlFlow controlFlow = MethodControlFlow.analyze(method);
            //当前方法的调用边收集
            Set<CallSite> calls = new LinkedHashSet<>();
            boolean terminal = false;

            for (AbstractInsnNode inst = method.instructions.getFirst(); inst != null; inst = inst.getNext()) {
                if (!controlFlow.isReachable(inst)) {
                    //无法到达的方法
                    continue;
                }
                if (inst instanceof MethodInsnNode call) {
                    //直接调用，添加
                    calls.add(new CallSite(call.owner, call.name, call.desc, call.getOpcode()));
                    if (isRemovalCall(call)) {
                        terminal = true;
                    }
                } else if (inst instanceof InvokeDynamicInsnNode indy) {
                    //间接调用，先获取Handle再添加
                    for (Object argument : indy.bsmArgs) {
                        if (argument instanceof Handle handle) {
                            int opcode = opcodeOf(handle);
                            if (opcode < 0) {
                                continue;
                            }
                            calls.add(new CallSite(handle.getOwner(), handle.getName(), handle.getDesc(), opcode));
                            if (isRemovalCall(handle)) {
                                terminal = true;
                            }
                        }
                    }
                } else if (isRemovalReasonWrite(inst)) {
                    //如果直接写入removalReason，也是收尾
                    terminal = true;
                }
            }

            //收集当前方法的信息
            methods.put(
                key(method.name, method.desc),
                new MethodInfo(List.copyOf(calls), terminal, controlFlow.isCertain())
            );
        }
        return new ClassInfo(node.name, node.superName, methods);
    }

    //是否为移除实体的方法
    private static boolean isRemovalCall(MethodInsnNode node) {
        return MethodConst.SET_REMOVED.compareWith(node)
            || MethodConst.REMOVE.compareWith(node)
            || MethodConst.KILL.compareWith(node)
            || MethodConst.DISCARD.compareWith(node);
    }

    //是否为移除实体的handle
    private static boolean isRemovalCall(Handle handle) {
        return MethodConst.SET_REMOVED.compareWith(handle)
            || MethodConst.REMOVE.compareWith(handle)
            || MethodConst.KILL.compareWith(handle)
            || MethodConst.DISCARD.compareWith(handle);
    }

    //是否直接写入移除原因
    private static boolean isRemovalReasonWrite(AbstractInsnNode instruction) {
        if (!(instruction instanceof FieldInsnNode field)
            || instruction.getOpcode() != Opcodes.PUTFIELD
            || !REMOVAL_REASON_DESC.equals(field.desc)) {
            //不是对应的field类，直接返回
            return false;
        }
        if (!REMOVAL_REASON_FIELD.equals(field.name) && !REMOVAL_REASON_FIELD_SRG.equals(field.name)) {
            //field名字不匹配
            return false;
        }
        AbstractInsnNode previous = previousReal(instruction);
        return previous == null || previous.getOpcode() != Opcodes.ACONST_NULL;
    }

    //获取这条指令的真实的前一条指令
    private static AbstractInsnNode previousReal(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous instanceof LabelNode || previous instanceof FrameNode || previous instanceof LineNumberNode) {
            previous = previous.getPrevious();
        }
        return previous;
    }

    /**
     * 获取handle对应的方法调用的opcode
     */
    public static int opcodeOf(Handle handle) {
        return switch (handle.getTag()) {
            case Opcodes.H_INVOKESTATIC -> Opcodes.INVOKESTATIC;
            case Opcodes.H_INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL -> Opcodes.INVOKESPECIAL;
            case Opcodes.H_INVOKEVIRTUAL -> Opcodes.INVOKEVIRTUAL;
            case Opcodes.H_INVOKEINTERFACE -> Opcodes.INVOKEINTERFACE;
            default -> -1;
        };
    }
}
