package com.oceanscenery.agent.patcher;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public final class LivingLootPatcher {
    private static final String HOOK = "com/oceanscenery/zenith/util/ForcedLoot";
    private static final String SOURCE = "(Lnet/minecraft/world/damagesource/DamageSource;";
    //"target":"METHOD",
    // "class":"net.minecraft.world.entity.LivingEntity",
    // "methodName":"m_6667_",
    // "methodDesc":"(Lnet/minecraft/world/damagesource/DamageSource;)V"
    private static final Set<String> ROOTS = Set.of(
        "dropAllDeathLoot" + SOURCE + ")V", "m_6668_" + SOURCE + ")V",
        "dropFromLootTable" + SOURCE + "Z)V", "m_7625_" + SOURCE + "Z)V",
        "dropCustomDeathLoot" + SOURCE + "IZ)V", "m_7472_" + SOURCE + "IZ)V"
    );
    private static final Set<String> SPAWN = Set.of("spawnAtLocation", "m_19983_", "m_19998_", "m_20000_", "m_5552_");
    private static final Type ITEM_ENTITY = Type.getObjectType("net/minecraft/world/entity/item/ItemEntity");

    private LivingLootPatcher() {
    }

    public static boolean transform(ClassNode owner) {
        Map<String, MethodNode> candidates = new HashMap<>();
        //待追踪的方法，初始为已知的掉落物方法
        Deque<MethodNode> pending = new ArrayDeque<>();
        for (MethodNode m : owner.methods) {
            //先扫描一遍所有方法，找到用于生成掉落物的方法
            if ((m.access & (ACC_STATIC | ACC_ABSTRACT | ACC_NATIVE)) != 0 || !m.desc.endsWith(")V")) {
                continue;
            }
            //掉落物方法直接加入
            if (ROOTS.contains(m.name + m.desc)) {
                pending.add(m);
            }
            //有方法体的方法都加入分析路径
            if (!m.name.startsWith("<")) {
                candidates.put(m.name + m.desc, m);
            }
        }
        //如果没有掉落物根方法就直接跳过
        if (pending.isEmpty()) {
            return false;
        }
        //已经分析过的方法，用于去重
        Set<MethodNode> visited = new HashSet<>();
        //记录每个方法会直接或间接到达掉落物方法的调用语句
        Map<MethodNode, Set<MethodInsnNode>> rewards = new HashMap<>();
        //缓存每个方法的Analyser分析结果
        Map<MethodNode, Frame<BasicValue>[]> frames = new HashMap<>();
        //用于记录一个方法调用的对应的方法，用于后续分析掉落战利品可达性
        Map<MethodInsnNode, MethodNode> helpers = new HashMap<>();
        while (!pending.isEmpty()) {
            MethodNode m = pending.remove();
            if (!visited.add(m)) {
                //不重复分析
                continue;
            }
            Frame<BasicValue>[] stack;
            try {
                stack = new Analyzer<>(new BasicInterpreter(ASM9) {
                    @Override
                    public BasicValue newValue(Type type) {
                        //专门保留物品实体的信息
                        return ITEM_ENTITY.equals(type) ? new BasicValue(type) : super.newValue(type);
                    }
                }).analyze(owner.name, m);//分析控制流
            } catch (AnalyzerException ignored) {
                continue;
            }
            //加入分析缓存
            frames.put(m, stack);
            //目前不知道有哪些掉落物语句，先插入一个空的集合
            Set<MethodInsnNode> calls = new HashSet<>();
            rewards.put(m, calls);
            for (AbstractInsnNode node : m.instructions) {
                if (!(node instanceof MethodInsnNode call)) {
                    //只收集方法调用语句
                    continue;
                }
                //是方法调用，获取其前一条栈信息
                Frame<BasicValue> before = stack[m.instructions.indexOf(call)];
                if (before == null) {
                    continue;
                }
                //满足下面条件的说明是掉落物语句
                //是掉落物语句，是生成物品实体的语句且返回值也是物品实体，或者是level.addFreshEntity语句且参数是物品实体
                if (ROOTS.contains(call.name + call.desc) || SPAWN.contains(call.name)
                    && call.desc.endsWith(")" + ITEM_ENTITY.getDescriptor())
                    || (call.name.equals("addFreshEntity") || call.name.equals("m_7967_"))
                    && call.desc.equals("(Lnet/minecraft/world/entity/Entity;)Z")
                    && ITEM_ENTITY.equals(before.getStack(before.getStackSize() - 1).getType())) {
                    calls.add(call);
                } else if (owner.name.equals(call.owner)) {
                    //如果是同类里的有方法体的语句,也加入分析
                    MethodNode helper = candidates.get(call.name + call.desc);
                    if (helper != null) {
                        //加入间接调用的分析
                        helpers.put(call, helper);
                        //加入待分析的队列
                        pending.add(helper);
                    }
                }
            }
        }
        //现在开始从确定的掉落物语句反向传递，标记所有路径上的方法
        boolean added;
        do {
            added = false;
            for (MethodNode m : rewards.keySet()) {
                for (AbstractInsnNode node : m.instructions) {
                    //如果是方法调用，且调用了helper方法，且helper方法调用的对应方法调用了战利品方法
                    if (node instanceof MethodInsnNode call && helpers.containsKey(call)
                        && !rewards.getOrDefault(helpers.get(call), Set.of()).isEmpty()) {
                        //加入方法，标记为产生变化
                        added |= rewards.get(m).add(call);
                    }
                }
            }
        } while (added); //如果不产生变化，说明稳定了，退出循环
        boolean changed = false;
        for (MethodNode m : rewards.keySet()) {
            if (!rewards.get(m).isEmpty() /*&& m.tryCatchBlocks.isEmpty()*/) {
                //如果这个方法调用了战利品方法，就插入强制跳转的语句
                changed |= patch(m, rewards.get(m), frames.get(m));
            }
        }
        return changed;
    }

    /**
     * 一个记录，用于记录要插桩的跳转语句，是强制跳转还是强制不跳转，以及是否要跳过else部分
     */
    record Choice(JumpInsnNode guard, boolean takeJump, JumpInsnNode skipElse) {
    }

    //针对一整个方法，让结果导向所有战利品语句
    private static boolean patch(MethodNode method, Set<MethodInsnNode> rewards, Frame<BasicValue>[] frames) {
        //原代码
        InsnList code = method.instructions;
        //保存的原本代码
        AbstractInsnNode[] original = code.toArray();
        for (AbstractInsnNode node : original) {
            //跳转指令跳到前面的指令，或者是switch语句，或者是钩子方法则不做处理
            if (node instanceof JumpInsnNode jump && code.indexOf(jump.label) <= code.indexOf(jump)
                || node instanceof TableSwitchInsnNode || node instanceof LookupSwitchInsnNode
                || node instanceof MethodInsnNode call && HOOK.equals(call.owner)) {
                return false;
            }
        }

        List<Choice> choices = new ArrayList<>();
        for (AbstractInsnNode node : original) {
            if (!(node instanceof JumpInsnNode guard) || guard.getOpcode() < IFEQ || guard.getOpcode() > IF_ICMPLE) {
                //如果不是跳转语句或者不是比较跳转语句
                continue;
            }
            //记录跳转语句下第一句和发生跳转时的第一条语句
            int from = code.indexOf(guard) + 1, target = code.indexOf(guard.label);
            //记录跳转目标前最后一句
            int last = target - 1;
            //由于存在栈帧等节点(opcode均为-1),所以需要从后往前找到第一条有效语句
            while (last >= from && original[last].getOpcode() < 0) {
                last--;
            }
            //要求两处入口处均为空栈,否则改写不安全
            if (last < from || !emptyStack(frames[from]) || !emptyStack(frames[target])) {
                continue;
            }
            AbstractInsnNode previous = guard.getPrevious();
            //获取跳转语句的有效的前一句
            while (previous != null && previous.getOpcode() < 0) {
                previous = previous.getPrevious();
            }
            //如果是instanceof就不去处理，否则容易出现转换异常
            if (previous != null && previous.getOpcode() == INSTANCEOF) {
                continue;
            }
            //跳转目标前面最后一个语句为goto是else的象征
            if (original[last] instanceof JumpInsnNode skip && skip.getOpcode() == GOTO) {
                int join = code.indexOf(skip.label);//找到else之后的指令
                //顺序运行，空栈保证栈平衡且两侧均有掉落物方法
                if (join > target && emptyStack(frames[last]) && straightReward(original, from, last, rewards)
                    && straightReward(original, target, join, rewards)) {
                    choices.add(new Choice(guard, false, skip));
                }
            } else if (straightReward(original, from, target, rewards)) {
                //不用跳转就有掉落物
                choices.add(new Choice(guard, false, null));
            } else if (firstOpcode(original, from, target) == RETURN
                && rewards.stream().anyMatch(call -> code.indexOf(call) >= target)) {
                //如果不跳转就会返回，则必须跳转来达到战利品语句
                choices.add(new Choice(guard, true, null));
            }
        }
        //正式插桩
        for (Choice choice : choices) {
            JumpInsnNode guard = choice.guard();
            int op = guard.getOpcode();
            InsnList hook = new InsnList();
            if (op >= IF_ICMPEQ) {
                hook.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "compare", "(II)I", false));
                op = IFEQ + (op - IF_ICMPEQ);
                guard.setOpcode(op);
            }
            int forced = switch (op) {
                case IFEQ, IFLE -> choice.takeJump() ? 0 : 1;
                case IFNE, IFGT -> choice.takeJump() ? 1 : 0;
                case IFLT -> choice.takeJump() ? -1 : 0;
                case IFGE -> choice.takeJump() ? 0 : -1;
                default -> throw new IllegalStateException();
            };
            hook.add(new VarInsnNode(ALOAD, 0));
            hook.add(new LdcInsnNode(forced));
            hook.add(new MethodInsnNode(INVOKESTATIC, HOOK, "operand", "(ILjava/lang/Object;I)I", false));
            code.insertBefore(guard, hook);
            if (choice.skipElse() != null && choice.skipElse().getOpcode() == GOTO) {
                InsnList check = new InsnList();
                check.add(new VarInsnNode(ALOAD, 0));
                check.add(new MethodInsnNode(INVOKESTATIC, HOOK, "active", "(Ljava/lang/Object;)Z", false));
                code.insertBefore(choice.skipElse(), check);
                choice.skipElse().setOpcode(IFEQ);
            }
        }
        if (!choices.isEmpty()) {
            method.maxStack += 2;
        }
        return !choices.isEmpty();
    }

    //检查帧是否为空
    private static boolean emptyStack(Frame<BasicValue> frame) {
        return frame != null && frame.getStackSize() == 0;
    }

    /**
     * 获取一段指令中两个位置之间最靠前的执行的语句
     *
     * @param nodes 指令
     * @param from  区间开头
     * @param end   区间结尾
     * @return 第一个指令的opcode,如果不存在则返回-1
     */
    private static int firstOpcode(AbstractInsnNode[] nodes, int from, int end) {
        while (from < end && nodes[from].getOpcode() < 0) {
            from++;
        }
        return from < end ? nodes[from].getOpcode() : -1;
    }

    private static boolean boundary(AbstractInsnNode node) {
        return node instanceof JumpInsnNode || node.getOpcode() == RETURN || node.getOpcode() == ATHROW;
    }

    private static boolean straightReward(AbstractInsnNode[] nodes, int from, int end, Set<MethodInsnNode> rewards) {
        boolean found = false;
        for (int i = from; i < end; i++) {
            AbstractInsnNode node = nodes[i];
            int op = node.getOpcode();
            //不能是边界指令或者存储类的局部变量指令
            if (boundary(node) || op >= ISTORE && op <= ASTORE || op == IINC) {
                return false;
            }
            //检查是否为战利品语句
            found |= rewards.contains(node);
        }
        return found;
    }
}
