package com.oceanscenery.agent.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 方法的控制流，存储：方法内可达指令的集合，以指令为节点的邻接表，代表起点的指令节点，分析结果，分析结果确定性
 */
public final class MethodControlFlow {
    //标识可达性的枚举
    public enum Reachability {
        UNREACHABLE, MAY_REACH, ALWAYS_REACH, UNKNOWN
    }

    //存储所有return类型的opcode的集合
    private static final Set<Integer> RETURN_OPCODES = Set.of(
        Opcodes.IRETURN,
        Opcodes.LRETURN,
        Opcodes.FRETURN,
        Opcodes.DRETURN,
        Opcodes.ARETURN,
        Opcodes.RETURN,
        Opcodes.ATHROW
    );

    //邻接表
    private final Map<AbstractInsnNode, List<AbstractInsnNode>> adjacency = new IdentityHashMap<>();
    //可达的所有指令
    private final Set<AbstractInsnNode> reachable;
    //入口指令
    private final AbstractInsnNode entry;
    //分析结果的确定性
    private final boolean certain;

    //构造
    private MethodControlFlow(Map<AbstractInsnNode, List<AbstractInsnNode>> adjacency
        , Set<AbstractInsnNode> reachable, AbstractInsnNode entry, boolean certain) {
        this.adjacency.putAll(adjacency);
        this.reachable = reachable;
        this.entry = entry;
        this.certain = certain;
    }

    //分析方法
    public static MethodControlFlow analyze(MethodNode method) {
        AbstractInsnNode entry = method.instructions.getFirst();
        if (entry == null) {
            //没有入口，方法是空的，返回空的分析流
            return new MethodControlFlow(Map.of(), newIdentitySet(), null, false);
        }
        //先标记为确定
        boolean certain = true;
        //邻接表
        Map<AbstractInsnNode, List<AbstractInsnNode>> edges = new IdentityHashMap<>();
        //遍历方法的所有指令
        for (AbstractInsnNode instruction = entry; instruction != null; instruction = instruction.getNext()) {
            List<AbstractInsnNode> next = new ArrayList<>(4);
            int opcode = instruction.getOpcode();

            if (instruction instanceof JumpInsnNode jump) {
                if (opcode == Opcodes.GOTO) {
                    add(next, jump.label);
                } else if (opcode == Opcodes.JSR) {
                    //jsr指令，不确定
                    add(next, jump.label);
                    add(next, instruction.getNext());
                    certain = false;
                } else if (isConditionalJump(opcode)) {
                    //if系列的跳转
                    add(next, jump.label);
                    add(next, instruction.getNext());
                } else {
                    //顺序执行
                    add(next, instruction.getNext());
                }
            } else if (instruction instanceof TableSwitchInsnNode sw) {
                //switch所有的处理都加入图中
                add(next, sw.dflt);
                for (LabelNode label : sw.labels) {
                    add(next, label);
                }
            } else if (instruction instanceof LookupSwitchInsnNode sw) {
                //switch所有的处理都加入图中
                add(next, sw.dflt);
                for (LabelNode label : sw.labels) {
                    add(next, label);
                }
            } else if (opcode == Opcodes.RET) {
                //依旧不确定
                certain = false;
            } else if (!RETURN_OPCODES.contains(opcode)) {
                //顺序执行
                add(next, instruction.getNext());
            }
            //把这个点的后继列表加到图中
            edges.put(instruction, List.copyOf(next));
        }
        //此时邻接表构建完成
        //找到所有可达节点
        Set<AbstractInsnNode> reachable = traverse(entry, edges, newIdentitySet());
        if (!method.tryCatchBlocks.isEmpty()) {
            //有异常处理，不确定是否能到达
            certain = false;
            //把异常控制加入到控制流图
            addExceptionHandlers(method, edges, reachable);
        }

        return new MethodControlFlow(edges, reachable, entry, certain);
    }

    //判断这个节点是否为控制流对象从第一条语句开始的的可达节点
    public boolean isReachable(AbstractInsnNode instruction) {
        return this.reachable.contains(instruction);
    }

    public boolean isCertain() {
        return this.certain;
    }

    /**
     * 添加异常边
     * @param method 正在处理的方法
     * @param edges 方法内部的控制流图（邻接表形式）
     * @param reachable 不通过异常控制流的所有可达语句（以方法的第一句为起点）
     */
    private static void addExceptionHandlers(MethodNode method, Map<AbstractInsnNode
        , List<AbstractInsnNode>> edges, Set<AbstractInsnNode> reachable) {
        //标记每条语句的行数（第几行）
        Map<AbstractInsnNode, Integer> indexes = new IdentityHashMap<>();
        int index = 0;
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null;
             instruction = instruction.getNext()) {
            indexes.put(instruction, index++);
        }

        boolean changed;
        do {
            changed = false;
            for (TryCatchBlockNode block : method.tryCatchBlocks) {
                //没有catch块或者catch块已经可达，不用重复添加
                if (block.handler == null || reachable.contains(block.handler)) {
                    continue;
                }

                //如果try块的开头不在方法里，就拉到最大，否则正常获取开头的行号
                int start = indexes.getOrDefault(block.start, Integer.MAX_VALUE);
                //如果try块的结尾不在方法里，就拉到最小，否则正常获取结尾的行号
                int end = indexes.getOrDefault(block.end, Integer.MIN_VALUE);
                //try块里的方法是否可达
                boolean tryReachable = false;
                for (AbstractInsnNode instruction : reachable) {
                    int current = indexes.getOrDefault(instruction, -1);
                    if (current >= start && current < end) {
                        //可达，直接退出循环
                        tryReachable = true;
                        break;
                    }
                }
                if (!tryReachable) {
                    //try块不可达，不需要添加边信息
                    continue;
                }

                //所有从catch块开始可以到达的节点
                Set<AbstractInsnNode> handlerFlow = traverse(
                    block.handler,//开头节点
                    edges,//邻接表
                    newIdentitySet()
                );
                for (AbstractInsnNode handlerInstruction : handlerFlow) {
                    if (reachable.add(handlerInstruction)) {
                        //catch块可达的边不是原来可达边的子集，发生变化
                        changed = true;
                    }
                }
            }
            //可能有嵌套的try块，继续处理直到稳定下来
        } while (changed);
    }

    /**
     * 从start开始遍历图，找到所有可达节点，放到最后一个参数中
     */
    private static Set<AbstractInsnNode> traverse(AbstractInsnNode start
        , Map<AbstractInsnNode, List<AbstractInsnNode>> edges, Set<AbstractInsnNode> output) {
        if (start == null) {
            //没有入口，返回空集
            return output;
        }
        Deque<AbstractInsnNode> queue = new ArrayDeque<>();
        queue.add(start);
        //广度优先遍历
        while (!queue.isEmpty()) {
            AbstractInsnNode current = queue.poll();
            if (!output.add(current)) {
                continue;
            }
            for (AbstractInsnNode next : edges.getOrDefault(current, List.of())) {
                if (next != null && !output.contains(next)) {
                    queue.add(next);
                }
            }
        }
        //可达节点
        return output;
    }

    //判断这个opcode是否为条件跳转语句
    private static boolean isConditionalJump(int opcode) {
        return switch (opcode) {
            case Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE,
                 Opcodes.IFGT, Opcodes.IFLE,
                 Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT,
                 Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE,
                 Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE,
                 Opcodes.IFNULL, Opcodes.IFNONNULL -> true;
            default -> false;
        };
    }

    //在添加之前判断是否为null，是则不添加
    private static void add(List<AbstractInsnNode> output, AbstractInsnNode instruction) {
        if (instruction != null) {
            output.add(instruction);
        }
    }

    private static Set<AbstractInsnNode> newIdentitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
