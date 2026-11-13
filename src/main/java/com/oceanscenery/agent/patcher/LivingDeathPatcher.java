package com.oceanscenery.agent.patcher;

import com.oceanscenery.agent.ClassInfo;
import com.oceanscenery.agent.DeathPathTracer;
import com.oceanscenery.agent.ZenithTransformer;
import com.oceanscenery.agent.util.MethodConst;
import com.oceanscenery.agent.util.MethodControlFlow;
import com.oceanscenery.agent.util.MethodData;
import com.oceanscenery.zenith.TheZenithMod;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class LivingDeathPatcher {
    private static final String MIXIN_EXTRAS_WRAPPED = "$mixinextras$wrapped$";
    private static final int MAX_CLOSURE_METHODS = 192;
    private static final boolean PATCH_LOG = Boolean.getBoolean("zenith.debug.death.patch");

    private static final String DEATH_TIME_NAME = "deathTime";
    private static final String DEATH_TIME_SRG_NAME = "f_20919_";

    //顺序为tickDeath父类,remove,setRemoved,kill,zenith$discard,五个tickDeath的可达分析目标
    private static final MethodData[] TICK_DEATH_GOALS = {
        MethodConst.TICK_DEATH,
        MethodConst.REMOVE,
        MethodConst.SET_REMOVED,
        MethodConst.KILL,
        MethodConst.DISCARD
    };

    //顺序为die父类,dropAllDeathLoot,remove,setRemoved,die的可达目标
    private static final MethodData[] DIE_GOALS = {
        MethodConst.DIE,
        MethodConst.DROP_LOOT,
        MethodConst.REMOVE,
        MethodConst.SET_REMOVED,
        MethodConst.KILL,
        MethodConst.DISCARD
    };

    private static final MethodData[] TERMINALS = {
        MethodConst.REMOVE, MethodConst.SET_REMOVED, MethodConst.KILL, MethodConst.DISCARD,
        MethodConst.DROP_LOOT
    };

    private static final String HOOK_OWNER = "com/oceanscenery/zenith/hooks/Hooks";
    private static final String INT_BRANCH = "jumpBranch";
    private static final String INT2_BRANCH = "compareBranch";
    private static final String REF_BRANCH = "nullBranch";
    private static final String REF2_BRANCH = "refCompareBranch";
    private static final String SWITCH_BRANCH = "switchBranch";
    private static final String DEATH_TIME_HOOK = "deathTimeModify";
    private static final String TICK_DEATH_HOOK = "onTickDeath";

    private static final String D_INT_BRANCH = "(ILjava/lang/Object;II)I";
    private static final String D_INT2_BRANCH = "(IILjava/lang/Object;II)I";
    private static final String D_REF_BRANCH = "(Ljava/lang/Object;Ljava/lang/Object;II)I";
    private static final String D_REF2_BRANCH = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;II)I";
    private static final String D_DEATH_TIME = "(ILjava/lang/Object;)I";
    private static final String D_DEATH_CLOCK = "(Ljava/lang/Object;)V";

    //死亡动画时间
    private static final int DEATH_ANIMATION_TICKS = 20;

    //Hooks的方法集合，用于判定一个方法是否已经插入了Hook方法
    private static final Set<String> CUS_HOOKS = Set.of(
        INT_BRANCH, INT2_BRANCH, REF_BRANCH, REF2_BRANCH, SWITCH_BRANCH,
        DEATH_TIME_HOOK, TICK_DEATH_HOOK
    );

    //和0比较的指令
    private static final Set<Integer> INT1 = Set.of(
        Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE
    );
    //两个整数进行比较的指令
    private static final Set<Integer> INT2 = Set.of(
        Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT,
        Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE
    );
    //和null进行比较的指令
    private static final Set<Integer> REF1 = Set.of(Opcodes.IFNULL, Opcodes.IFNONNULL);
    //两个引用进行比较的指令
    private static final Set<Integer> REF2 = Set.of(Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE);

    //tickDeath有5个目标，对应数组编号0-4，从5开始为非直接目标
    private static final int INDIRECT_TIER = 5;

    //没有找到收尾目标的信息，包含类名，方法和这个方法的描述符
    private static final Set<String> WARNED_NO_GOAL = ConcurrentHashMap.newKeySet();

    //方法调用的上下文（由tickDeath调用或是由die调用）
    private enum DeathContext {
        TICK_DEATH,
        DIE
    }

    //目标的名称
    private final Set<String> targetNames;

    //
    private final Map<ClassLoader, DeathPathTracer> tracers = new IdentityHashMap<>();

    //通过方法的名字和描述构建一个唯一的key
    private record MethodKey(String name, String desc) {
        static MethodKey of(MethodNode method) {
            return new MethodKey(method.name, method.desc);
        }
    }

    public LivingDeathPatcher(Set<String> targets) {
        this.targetNames = Set.copyOf(targets);
    }

    public boolean transform(ClassNode classNode, String targetName, ClassLoader loader) {
        if (!this.targetNames.contains(targetName)) {
            return false;
        }
        if (ZenithTransformer.ENTITY.equals(targetName)) {
            return false;
        }

        // 进入分析前移除旧的类信息，防止使用 patch 前的 ClassInfo。
        invalidateDeathCaches(classNode.name);

        //类的所有方法
        Map<MethodKey, MethodNode> methods = new LinkedHashMap<>();
        for (MethodNode method : classNode.methods) {
            methods.put(MethodKey.of(method), method);
        }
        //所有方法的调用上下文(被{tickDeath,die}中的哪些方法调用过)
        Map<MethodKey, Set<DeathContext>> contexts = new HashMap<>();
        for (MethodNode method : classNode.methods) {
            if (isTickDeath(method)) {
                //tickDeath本身,直接添加
                addContext(contexts, MethodKey.of(method), DeathContext.TICK_DEATH);
            } else if (isDie(method)) {
                //die本身,直接添加
                addContext(contexts, MethodKey.of(method), DeathContext.DIE);
            }
        }
        //获取调用图

        //如果没有die或tickDeath方法，且不写入deathTime，那就是无死亡方法
        boolean hasDeathRoot = !contexts.isEmpty();
        boolean hasDeathTimeWrite = hasDeathTimeWrite(classNode);

        if (!hasDeathRoot && !hasDeathTimeWrite) {
            //无死亡方法
            return false;
        }

        //检查是否有死亡方法的调用
        Map<MethodKey, Set<MethodKey>> callers = hasDeathRoot
            ? collectClassCallers(classNode)
            : Map.of();

        if (hasDeathRoot) {
            discoverDeathClosure(classNode, methods, contexts);
        }

        boolean changed = false;
        for (MethodNode method : classNode.methods) {
            //遍历当前类的method
            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                continue;
            }

            MethodKey methodKey = MethodKey.of(method);
            //获取当前方法的上下文
            Set<DeathContext> methodContexts = contexts.get(methodKey);
            //分别判断是否为死亡类的方法本体
            boolean rootTickDeath = isTickDeath(method);
            boolean rootDie = isDie(method);
            //判断是否为死亡方法所调用的方法
            boolean deathMethod = methodContexts != null;

            //先去处理deathTime的写入
            boolean local = beforeModifyDeathTime(method);

            if (rootTickDeath) {
                //对tickDeath额外注入一下deathTime计时器
                local |= injectDeathClock(method);
            }

            if (methodContexts != null
                && (rootTickDeath || rootDie || isDeathDedicated(methodKey, callers, contexts))) {
                //有上下文（被调用过），且只被一种死亡方法调用过
                //获取此方法唯一的上下文
                DeathContext context = selectContext(methodContexts);
                if (context == null) {
                    if (PATCH_LOG) {
                        TheZenithMod.LOGGER.info(
                            "[Zenith] skip ambiguous death helper {}.{}{} contexts={}",
                            classNode.name,
                            method.name,
                            method.desc,
                            methodContexts
                        );
                    }
                    continue;
                }

                //目标方法由上下文决定
                MethodData[] goals = context == DeathContext.TICK_DEATH
                    ? TICK_DEATH_GOALS
                    : DIE_GOALS;
                int primaryGate = 0;
                int fallbackGate = context == DeathContext.TICK_DEATH
                    ? DEATH_ANIMATION_TICKS
                    : 0;
                GoalMatch match = findGoals(method.instructions, goals);
                if (match == null) {
                    //没有直接的匹配，
                    match = traceIndirect(classNode, method, loader);
                }
                local |= instrumentToGoal(classNode.name, method, match, primaryGate, fallbackGate);
                if (match == null && (rootTickDeath || rootDie)) {
                    //没有直接匹配，且是要注入的原版方法，发出警告
                    warnNoGoal(classNode.name, method);
                }
            }

            if (local) {
                changed = true;
            }
        }
        if (PATCH_LOG && contexts.size() > 1) {
            TheZenithMod.LOGGER.info(
                "[Zenith] {} death closure: {} method(s), changed={}",
                classNode.name,
                contexts.size(),
                changed
            );
        }
        if (changed) {
            // 当前类的字节码已变化，旧 ClassInfo 不能继续复用。
            invalidateDeathCaches(classNode.name);
        }
        return changed;
    }

    private GoalMatch traceIndirect(ClassNode classNode, MethodNode method, ClassLoader loader) {
        //先获取追踪结果
        DeathPathTracer.TraceResult result = tracer(loader).trace(classNode, method);
        if (result.status() == DeathPathTracer.Status.FOUND) {
            //找到了
            return new GoalMatch(result.goalCalls(), INDIRECT_TIER);
        }
        //下面要么是没找到，要么是不确定
        if (result.status() == DeathPathTracer.Status.UNCERTAIN && DeathPathTracer.TRACE_LOG) {
            TheZenithMod.LOGGER.info(
                "[Zenith] {} 的 {} 不确定收尾路径存在性",
                classNode.name, method.name
            );
        } else if (result.status() == DeathPathTracer.Status.NONE && DeathPathTracer.TRACE_LOG) {
            TheZenithMod.LOGGER.info(
                "[Zenith] {} 的 {} 无可达收尾路径",
                classNode.name, method.name
            );
        }
        return null;
    }

    /**
     * 用于从一个类中找到所有由死亡方法开始调用的方法，并记录上下文
     *
     * @param classNode 类
     * @param methods   类的方法映射(method key->method),当遍历涉及某个方法时,需要从方法名获取方法,然后分析,递归进行得到方法的方法调用
     * @param contexts  初始为die和tickDeath,函数执行完后会包含所有死亡方法涉及到的方法(仅限类的内部)
     */
    private static void discoverDeathClosure(ClassNode classNode
        , Map<MethodKey, MethodNode> methods, Map<MethodKey, Set<DeathContext>> contexts) {
        //一个记录，用于标识一个方法调用和调用时的上下文（一个方法被不同上下文调用要处理两遍）
        record Work(MethodKey key, DeathContext context) {
        }

        //要进行遍历分析的方法
        Deque<Work> queue = new ArrayDeque<>();
        //遍历调用上下文
        for (Map.Entry<MethodKey, Set<DeathContext>> entry : contexts.entrySet()) {
            for (DeathContext context : entry.getValue()) {
                //把映射key->set<value>转化成若干个key->value并加入队列（此时应当只有die和tickDeath）
                queue.add(new Work(entry.getKey(), context));
            }
        }

        //所有处理过的方法，防止重复处理
        Set<String> processed = new HashSet<>();
        //所有被调用过（有调用上下文）的方法的数量，同时也是一个计数器
        int discovered = contexts.size();
        while (!queue.isEmpty() && discovered <= MAX_CLOSURE_METHODS) {
            //还有方法没有处理，且处理的方法数还没有到达上限
            Work work = queue.poll();
            String workKey = work.key() + work.context().name();
            if (!processed.add(workKey)) {
                continue;
            }
            //获取调用的方法
            MethodNode method = methods.get(work.key());
            if (method == null) {
                //不存在或者不属于本类
                continue;
            }
            MethodControlFlow flow = MethodControlFlow.analyze(method);
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {
                //对于到达不了的语句，直接跳过
                if (!flow.isReachable(instruction)) {
                    continue;
                }
                if (instruction instanceof MethodInsnNode call) {
                    //方法调用，如果还没有加入过，就加入分析列表
                    MethodKey target = helperTarget(
                        classNode,
                        methods,
                        call.owner,
                        call.name,
                        call.desc
                    );
                    if (target != null
                        && addContext(contexts, target, work.context())) {
                        //如果合适且没有被加入到调用上下文的map尚未记录这个调用
                        queue.add(new Work(target, work.context()));
                        discovered++;
                    }
                } else if (instruction instanceof InvokeDynamicInsnNode indy) {
                    //间接调用，和直接方法调用同理
                    for (Object argument : indy.bsmArgs) {
                        if (!(argument instanceof Handle handle)) {
                            continue;
                        }
                        int opcode = ClassInfo.opcodeOf(handle);
                        if (opcode < 0) {
                            continue;
                        }
                        MethodKey target = helperTarget(
                            classNode,
                            methods,
                            handle.getOwner(),
                            handle.getName(),
                            handle.getDesc()
                        );
                        if (target != null
                            && addContext(contexts, target, work.context())) {
                            queue.add(new Work(target, work.context()));
                            discovered++;
                        }
                    }
                }
            }
        }
        if (discovered > MAX_CLOSURE_METHODS || !queue.isEmpty()) {
            TheZenithMod.LOGGER.warn(
                "[Zenith] {} death helper closure reached safety limit {}",
                classNode.name,
                MAX_CLOSURE_METHODS
            );
        }
    }

    /**
     * 获取某个方法对应的key（同时稍微判断这个方法是否为helper方法）
     *
     * @param classNode 类的对象
     * @param methods   类的所有方法
     * @param owner     此方法的所属类
     * @param name      此方法的方法名
     * @param desc      此方法的描述符
     * @return MethodKey对象，如果此方法不该被分析，就返回null
     */
    private static MethodKey helperTarget(ClassNode classNode
        , Map<MethodKey, MethodNode> methods, String owner, String name, String desc) {
        if (!classNode.name.equals(owner)
            || "<init>".equals(name)
            || "<clinit>".equals(name)
            || isTerminalName(name)) {
            //如果不是本类方法，或是初始化方法，或是移除类的方法，都不属于第三方的helper
            return null;
        }
        MethodKey key = new MethodKey(name, desc);
        MethodNode target = methods.get(key);
        /*(target.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC)) == 0 && */
        if (target == null || (target.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
            //如果是不合适的目标，也不做处理
            return null;
        }
        return key;
    }

    /**
     * 获取一个类中 所有方法->所有可能会调用它的方法的集合 的映射
     *
     * @param classNode 要分析的类
     * @return 映射表
     */
    private static Map<MethodKey, Set<MethodKey>> collectClassCallers(ClassNode classNode) {
        //此map的key为某个本类的方法，value为所有调用了它的方法
        Map<MethodKey, Set<MethodKey>> callers = new HashMap<>();
        for (MethodNode caller : classNode.methods) {
            //遍历所有方法
            MethodKey callerKey = MethodKey.of(caller);
            //获取方法的控制流信息
            MethodControlFlow flow = MethodControlFlow.analyze(caller);
            for (AbstractInsnNode instruction = caller.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {
                //找到方法内部的每条指令
                if (!flow.isReachable(instruction)) {
                    //不可到达的指令不做考虑
                    continue;
                }
                if (instruction instanceof MethodInsnNode call
                    && classNode.name.equals(call.owner)) {
                    //是来自于本类的方法调用
                    callers.computeIfAbsent(
                        new MethodKey(call.name, call.desc),
                        key -> new HashSet<>()
                    ).add(callerKey);
                } else if (instruction instanceof InvokeDynamicInsnNode indy) {
                    //如法炮制，先拆出方法调用，再加入map
                    for (Object argument : indy.bsmArgs) {
                        if (argument instanceof Handle handle
                            && classNode.name.equals(handle.getOwner())) {
                            callers.computeIfAbsent(
                                new MethodKey(handle.getName(), handle.getDesc()),
                                key -> new HashSet<>()
                            ).add(callerKey);
                        }
                    }
                }
            }
        }
        return callers;
    }

    private static boolean isDeathDedicated(
        MethodKey key,
        Map<MethodKey, Set<MethodKey>> callers,
        Map<MethodKey, Set<DeathContext>> contexts
    ) {
        Set<MethodKey> methodCallers = callers.get(key);
        if (methodCallers == null || methodCallers.isEmpty()) {
            return false;
        }
        for (MethodKey caller : methodCallers) {
            if (!contexts.containsKey(caller)) {
                return false;
            }
        }
        return true;
    }

    private static DeathContext selectContext(Set<DeathContext> contexts) {
        if (contexts.size() != 1) {
            return null;
        }
        return contexts.iterator().next();
    }

    private static boolean addContext(Map<MethodKey, Set<DeathContext>> contexts, MethodKey key, DeathContext context) {
        return contexts.computeIfAbsent(key, ignored -> EnumSet.noneOf(DeathContext.class)).add(context);
    }

    //收尾目标（除了die和tickDeath本身）
    private static boolean isTerminalName(String name) {
        for (MethodData data : TERMINALS) {
            if (data.nameEquals(name)) {
                return true;
            }
        }
        return false;
    }

    private DeathPathTracer tracer(ClassLoader loader) {
        synchronized (this.tracers) {
            return this.tracers.computeIfAbsent(loader, DeathPathTracer::new);
        }
    }

    /**
     * 当前类的字节码变化后，只失效这个类的 tracer 缓存。
     */
    private void invalidateDeathCaches(String internalName) {
        synchronized (this.tracers) {
            for (DeathPathTracer tracer : this.tracers.values()) {
                tracer.invalidate(internalName);
            }
        }
    }

    public void clearCaches() {
        synchronized (this.tracers) {
            this.tracers.values().forEach(DeathPathTracer::clearCache);
            this.tracers.clear();
        }
    }

    private static void warnNoGoal(String owner, MethodNode method) {
        if (WARNED_NO_GOAL.add(owner + "#" + method.name + method.desc)) {
            TheZenithMod.LOGGER.warn(
                "[Zenith] {} 的 {} 没有可用的收尾目标",
                owner, method.name
            );
        }
    }

    private static boolean beforeModifyDeathTime(MethodNode method) {
        boolean changed = false;
        for (AbstractInsnNode node = method.instructions.getFirst(); node != null; node = node.getNext()) {
            if (!writeDeathTime(node) || alreadyHooked(node)) {
                continue;
            }

            InsnList patch = new InsnList();
            patch.add(new InsnNode(Opcodes.DUP2));
            patch.add(new InsnNode(Opcodes.POP));
            patch.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC, HOOK_OWNER, DEATH_TIME_HOOK, D_DEATH_TIME, false));
            method.instructions.insertBefore(node, patch);
            changed = true;
        }
        return changed;
    }

    private static boolean writeDeathTime(AbstractInsnNode node) {
        return node.getOpcode() == Opcodes.PUTFIELD
            && node instanceof FieldInsnNode field
            && "I".equals(field.desc)
            && (DEATH_TIME_NAME.equals(field.name) || DEATH_TIME_SRG_NAME.equals(field.name));
    }

    private static boolean injectDeathClock(MethodNode method) {
        AbstractInsnNode node = method.instructions.getFirst();
        for (int i = 0; i < 3 && node != null; i++, node = node.getNext()) {
            if (node instanceof MethodInsnNode call
                && HOOK_OWNER.equals(call.owner)
                && TICK_DEATH_HOOK.equals(call.name)) {
                return false;
            }
        }

        AbstractInsnNode first = method.instructions.getFirst();
        if (first == null) {
            return false;
        }
        InsnList patch = new InsnList();
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
        patch.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC, HOOK_OWNER, TICK_DEATH_HOOK, D_DEATH_CLOCK, false));
        method.instructions.insertBefore(first, patch);
        return true;
    }

    /**
     * 负责将控制流的路径导向目标方法调用
     *
     * @param owner        类名
     * @param method       方法
     * @param match        方法调用的匹配项（即目标）
     * @param primaryGate  一般为0，默认方法允许在多少tick后强制导向目标tick
     * @param fallbackGate 一般为20，控制最终的结尾方法在多少tick后强制执行
     * @return 是否正常修改了方法
     */
    private static boolean instrumentToGoal(String owner, MethodNode method, GoalMatch match, int primaryGate, int fallbackGate) {
        if (match == null) {
            return false;
        }
        method.maxStack += 4;
        //内部的分析类，用于获取控制流图
        InnerAnalyzer analyzer = new InnerAnalyzer();
        try {
            analyzer.analyze(owner, method);
        } catch (AnalyzerException e) {
            throw new IllegalStateException("Unsuccessful bytecode analysis: " + owner + "." + method.name + method.desc, e);
        }

        InsnList insns = method.instructions;
        Set<AbstractInsnNode> goals = match.nodes();
        //这里读取deathTime条件
        int minDeathTime = match.tier() == 0 ? primaryGate : fallbackGate;

        //这里构建邻接表，来分析控制流
        Map<AbstractInsnNode, List<AbstractInsnNode>> predecessors = new IdentityHashMap<>();
        for (int edgeIndex = 0; edgeIndex < analyzer.normalEdgeCount; edgeIndex += 2) {
            //normalEdges为一个数组，两两一组表示一条边的始末，[1,2,3,4]表示1-2，3-4的两条调用边
            AbstractInsnNode from = insns.get(analyzer.normalEdges[edgeIndex]);
            AbstractInsnNode to = insns.get(analyzer.normalEdges[edgeIndex + 1]);
            //这是一个反向邻接表
            predecessors.computeIfAbsent(to, key -> new ArrayList<>()).add(from);
        }

        Set<AbstractInsnNode> canReach = newIdentitySet();
        //把所有终点推入队列
        Deque<AbstractInsnNode> queue = new ArrayDeque<>(goals);
        //为什么canReach加入重点而非起点，因为要从终点开始图的搜索，所以终点才是搜索的起点
        canReach.addAll(goals);
        while (!queue.isEmpty()) {
            AbstractInsnNode current = queue.poll();
            for (AbstractInsnNode previous : predecessors.getOrDefault(current, List.of())) {
                if (canReach.add(previous)) {
                    queue.add(previous);
                }
            }
        }
        //搜索完了之后所有可达的语句都收集完毕
        //现在收集全部分支语句
        List<AbstractInsnNode> branches = new ArrayList<>();
        for (AbstractInsnNode node : insns) {
            if (isBranch(node)) {
                branches.add(node);
            }
        }

        boolean changed = false;
        for (AbstractInsnNode branch : branches) {
            if (alreadyHooked(branch)) {
                //不去处理已经处理过的语句
                continue;
            }

            //选取后继
            AbstractInsnNode chosen = null;
            int distinct = 0;//总共有几种可能（if一般2，switch未知）
            int reachable = 0;//可到达目标的可能数
            Set<AbstractInsnNode> seen = newIdentitySet();//用于去重
            for (AbstractInsnNode successor : successorsOf(insns, branch)) {
                if (!seen.add(successor)) {
                    continue;
                }
                distinct++;
                if (canReach.contains(successor)) {
                    //如果这条语句处在可到达的集合（可能到达而非必须到达）里，那就不用作处理
                    reachable++;
                    if (chosen == null) {
                        //永远记录第一个可达的选择分支
                        chosen = successor;
                    }
                }
            }
            if (chosen == null || reachable == distinct) {
                //如果都不可达（前者）或者都可达（后者），就不做处理
                continue;
            }
            //有分支无法到达，需要手动插入
            changed |= instrument(method, branch, chosen, minDeathTime);
        }
        //结束
        return changed;
    }

    //标记
    private record GoalMatch(Set<AbstractInsnNode> nodes, int tier) {
    }

    //从指令列表中找到目标
    private static GoalMatch findGoals(InsnList insns, MethodData[] goalTiers) {
        for (int tier = 0; tier < goalTiers.length; tier++) {
            MethodData data = goalTiers[tier];
            Set<AbstractInsnNode> found = newIdentitySet();
            for (AbstractInsnNode node : insns) {
                if (node instanceof MethodInsnNode call
                    && data.compareWith(call)) {
                    if (tier == 0 && call.getOpcode() != Opcodes.INVOKESPECIAL) {
                        //不是父类方法且同名，是自递归或者helper，不视为目标
                        continue;
                    }
                    found.add(call);
                }
            }
            if (!found.isEmpty()) {
                return new GoalMatch(found, tier);
            }
        }
        return null;
    }

    /**
     * 用于将分支语句导向可以到达目标的语句
     *
     * @param method       方法
     * @param branch       分支语句
     * @param chosen       目标分支（可以到达目标的第一个分支）
     * @param minDeathTime 强制导向的最小deathTime
     * @return 是否修改了方法
     */
    private static boolean instrument(MethodNode method, AbstractInsnNode branch, AbstractInsnNode chosen, int minDeathTime) {
        String hook;
        String descriptor;
        int forced;

        if (branch instanceof JumpInsnNode jump) {
            //首先处理跳转语句（ifeq等）
            int opcode = jump.getOpcode();
            //是否跳转；取决于目标分支和跳转的分支是否为同一个
            boolean taken = chosen == jump.label;

            if (INT1.contains(opcode)) {
                //只有一个操作数的语句，选择对应的辅助方法，其它分支同理
                hook = INT_BRANCH;
                descriptor = D_INT_BRANCH;
            } else if (INT2.contains(opcode)) {
                hook = INT2_BRANCH;
                descriptor = D_INT2_BRANCH;
            } else if (REF1.contains(opcode)) {
                hook = REF_BRANCH;
                descriptor = D_REF_BRANCH;
            } else if (REF2.contains(opcode)) {
                hook = REF2_BRANCH;
                descriptor = D_REF2_BRANCH;
            } else {
                return false;
            }

            //把所有跳转语句转化为单操作数的跳转语句
            int normalized = normalize(opcode);
            if (normalized != opcode) {
                //设置一下新的opcode
                jump.setOpcode(normalized);
            }
            //这个变量是用于强制跳转的参数
            forced = taken ? forcedTaken(normalized) : forcedNotTaken(normalized);
        } else if (branch instanceof TableSwitchInsnNode sw) {
            hook = SWITCH_BRANCH;
            descriptor = D_INT_BRANCH;
            forced = forcedKeyForTable(sw, chosen);
        } else if (branch instanceof LookupSwitchInsnNode sw) {
            hook = SWITCH_BRANCH;
            descriptor = D_INT_BRANCH;
            forced = forcedKeyForLookup(sw, chosen);
        } else {
            return false;
        }

        InsnList patch = new InsnList();
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));//加载局部变量（? extends LivingEntity）
        patch.add(new LdcInsnNode(forced));//加载常数forced
        patch.add(new LdcInsnNode(minDeathTime));//加载常数：最小生效的deathTime
        patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK_OWNER, hook, descriptor, false));//插入方法调用
        method.instructions.insertBefore(branch, patch);//插入
        return true;
    }

    /**
     * 选取一个分支语句的所有可能的下一条语句（指令）
     *
     * @param insns  整个方法的所有语句
     * @param branch 分支语句
     * @return 后继指令构成的列表
     */
    private static List<AbstractInsnNode> successorsOf(InsnList insns, AbstractInsnNode branch) {
        List<AbstractInsnNode> successors = new ArrayList<>(4);
        if (branch instanceof JumpInsnNode jump) {
            successors.add(jump.label);
            int next = insns.indexOf(jump) + 1;
            if (next < insns.size()) {
                successors.add(insns.get(next));
            }
        } else if (branch instanceof TableSwitchInsnNode sw) {
            successors.add(sw.dflt);
            successors.addAll(sw.labels);
        } else if (branch instanceof LookupSwitchInsnNode sw) {
            successors.add(sw.dflt);
            successors.addAll(sw.labels);
        }
        return successors;
    }

    //统一形式为和0比较
    private static int normalize(int opcode) {
        if (INT1.contains(opcode)) {
            return opcode;
        }
        return switch (opcode) {
            case Opcodes.IF_ICMPEQ -> Opcodes.IFEQ;
            case Opcodes.IF_ICMPNE -> Opcodes.IFNE;
            case Opcodes.IF_ICMPLT -> Opcodes.IFLT;
            case Opcodes.IF_ICMPGE -> Opcodes.IFGE;
            case Opcodes.IF_ICMPGT -> Opcodes.IFGT;
            case Opcodes.IF_ICMPLE -> Opcodes.IFLE;
            case Opcodes.IFNULL -> Opcodes.IFNE;
            case Opcodes.IFNONNULL -> Opcodes.IFEQ;
            case Opcodes.IF_ACMPEQ -> Opcodes.IFNE;
            case Opcodes.IF_ACMPNE -> Opcodes.IFEQ;
            default -> opcode;
        };
    }

    /**
     * 获取这个操作码对应的一定会跳转的值
     */
    private static int forcedTaken(int normalizedCode) {
        return switch (normalizedCode) {
            case Opcodes.IFEQ, Opcodes.IFGE, Opcodes.IFLE -> 0;
            case Opcodes.IFNE, Opcodes.IFGT -> 1;
            case Opcodes.IFLT -> -1;
            default -> 0;
        };
    }

    /**
     * 获取这个操作码对应的一定不会跳转的值
     */
    private static int forcedNotTaken(int normalizedCode) {
        return switch (normalizedCode) {
            case Opcodes.IFEQ, Opcodes.IFLE -> 1;
            case Opcodes.IFNE, Opcodes.IFLT -> 0;
            case Opcodes.IFGE, Opcodes.IFGT -> -1;
            default -> 0;
        };
    }

    /**
     * 获取table switch对应的强制跳转的操作数
     */
    private static int forcedKeyForTable(TableSwitchInsnNode sw, AbstractInsnNode chosen) {
        for (int i = 0; i < sw.labels.size(); i++) {
            if (sw.labels.get(i) == chosen) {
                return sw.min + i;
            }
        }
        if (sw.min != Integer.MIN_VALUE) {
            return sw.min - 1;
        }
        if (sw.max != Integer.MAX_VALUE) {
            return sw.max + 1;
        }
        TheZenithMod.LOGGER.error(
            "failed to find right key for switch table", new IllegalStateException()
        );
        return 0;
    }

    /**
     * 获取lookup switch对应的强制跳转的操作数
     */
    private static int forcedKeyForLookup(LookupSwitchInsnNode sw, AbstractInsnNode chosen) {
        for (int i = 0; i < sw.labels.size(); i++) {
            if (sw.labels.get(i) == chosen) {
                //如果不在default分支，就选择对应的分支
                return sw.keys.get(i);
            }
        }
        Set<Integer> keys = new HashSet<>(sw.keys);
        for (int candidate : new int[]{0, -1, 1, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            if (!keys.contains(candidate)) {
                //处在default分支，就返回一个不属于任何分支的操作数
                return candidate;
            }
        }
        TheZenithMod.LOGGER.error(
            "failed to find right key for switch table", new IllegalStateException()
        );
        return 0;
    }

    //跳转指令？
    private static boolean isBranch(AbstractInsnNode node) {
        if (node instanceof JumpInsnNode) {
            int opcode = node.getOpcode();
            return INT1.contains(opcode) || INT2.contains(opcode)
                || REF1.contains(opcode) || REF2.contains(opcode);
        }
        return node instanceof TableSwitchInsnNode || node instanceof LookupSwitchInsnNode;
    }

    /**
     * 如果分支前面的语句是辅助方法调用，那么说明这个分支处被注入过，此时停止进一步处理
     *
     * @param node 一个分支节点
     */
    private static boolean alreadyHooked(AbstractInsnNode node) {
        AbstractInsnNode previous = node.getPrevious();
        return previous instanceof MethodInsnNode call
            && HOOK_OWNER.equals(call.owner)
            && CUS_HOOKS.contains(call.name);
    }

    private static Set<AbstractInsnNode> newIdentitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static boolean isTickDeath(MethodNode method) {
        return MethodConst.TICK_DEATH.compareWith(method)
            && (method.access & Opcodes.ACC_STATIC) == 0;
    }

    private static boolean isDie(MethodNode method) {
        return MethodConst.DIE.compareWith(method)
            && (method.access & Opcodes.ACC_STATIC) == 0;
    }

    private static boolean hasDeathTimeWrite(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof FieldInsnNode field
                    && insn.getOpcode() == Opcodes.PUTFIELD
                    && "I".equals(field.desc)
                    && (DEATH_TIME_NAME.equals(field.name)
                    || DEATH_TIME_SRG_NAME.equals(field.name))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static final class InnerAnalyzer extends Analyzer<BasicValue> {
        int[] normalEdges = new int[16];
        int normalEdgeCount;

        public InnerAnalyzer() {
            super(new BasicInterpreter());
        }

        @Override
        protected void newControlFlowEdge(int insnIndex, int successorIndex) {
            if (this.normalEdgeCount + 2 > this.normalEdges.length) {
                this.normalEdges = Arrays.copyOf(
                    this.normalEdges,
                    this.normalEdges.length * 2
                );
            }
            this.normalEdges[this.normalEdgeCount++] = insnIndex;
            this.normalEdges[this.normalEdgeCount++] = successorIndex;
        }

        @Override
        protected boolean newControlFlowExceptionEdge(int insnIndex, int successorIndex) {
            return super.newControlFlowExceptionEdge(insnIndex, successorIndex);
        }
    }
}
