package com.oceanscenery.agent;

import com.oceanscenery.agent.util.MethodControlFlow;
import com.oceanscenery.zenith.TheZenithMod;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//用于追踪死亡流程的跨方法调用
public final class DeathPathTracer {
    //是否找到收尾目标的标记
    public enum Status {
        FOUND, NONE, UNCERTAIN
    }

    /**
     * 追踪结果：包含是否追踪成功，目标方法调用（即哪些方法最后可以到达预期的收尾结果），以及调用的方法路径（比如die->customProcess()->Helper.remove()）
     * @param status 是否找到目标这个结果对应的状态
     * @param goalCalls 找到的所有收尾目标
     * @param chainDesc 调用链描述
     */
    public record TraceResult(Status status, Set<AbstractInsnNode> goalCalls, String chainDesc) {
    }

    /**
     * -Dzenith.debug.death.trace=true
     * 用于开启追踪日志的选项
     */
    public static final boolean TRACE_LOG = Boolean.getBoolean("zenith.debug.death.trace");

    private static final int MAX_DEPTH = 6; //最大追踪的方法深度
    private static final int MAX_METHODS = 192; //最多收集的方法个数
    private static final int MAX_CALLS_PER_METHOD = 64; //每个方法最多的调用次数
    private static final int MAX_SUBCLASSES = 24; //最大寻找的子类数量

    //待处理的类的类加载器，可以用来正确加载类
    private final ClassLoader loader;
    //类信息的缓存
    private final Map<String, Optional<ClassInfo>> classCache = new ConcurrentHashMap<>();
    //运行时经过处理后（保险）的类信息
    private final Map<String, ClassInfo> runtimeClasses = new ConcurrentHashMap<>();

    public DeathPathTracer(ClassLoader loader) {
        this.loader = loader;
    }

    //追踪一个类某个方法中是否存在间接收尾的方法
    public TraceResult trace(ClassNode owner, MethodNode method) {
        //从类节点加载类信息
        ClassInfo currentClass = ClassInfo.fromClassNode(owner);
        //存储运行时类信息
        this.runtimeClasses.put(owner.name, currentClass);
        //用来收集目标（如果某个调用能够收尾）的集合，目前为空
        Set<AbstractInsnNode> goals = Collections.newSetFromMap(new IdentityHashMap<>());
        //获取方法的控制流
        MethodControlFlow rootFlow = MethodControlFlow.analyze(method);
        boolean uncertain = false;
        //调用链信息
        String chain = null;
        int scanned = 0;

        //遍历所有指令
        for (AbstractInsnNode inst = method.instructions.getFirst(); inst != null; inst = inst.getNext()) {
            if (!(inst instanceof MethodInsnNode) && !(inst instanceof InvokeDynamicInsnNode)) {
                //不是任何直接或间接的方法调用
                continue;
            }
            if (!rootFlow.isReachable(inst)) {
                //无法到达的节点不做分析
                continue;
            }
            //是有效方法调用，有数量上限，超过数量直接停止扫描
            if (++scanned > MAX_CALLS_PER_METHOD) {
                uncertain = true;
                break;
            }

            //一个栈，用来存储字符串形式的调用栈
            Deque<String> path = new ArrayDeque<>();
            int[] budget = {0};
            Status status = checkCallSite(
                inst, 1, new HashSet<>(), path, budget
            );
            if (status == Status.FOUND) {
                //找到目标，加入到收集的目标列表
                goals.add(inst);
                if (chain == null) {
                    //修改调用链的描述
                    chain = simpleName(owner.name) + "." + method.name + " -> " + String.join(" -> ", path);
                }
            } else if (status == Status.UNCERTAIN) {
                //结果不确定，标记
                uncertain = true;
            }
        }

        //根据状态判断结果
        Status result = !goals.isEmpty() ? Status.FOUND :
            (uncertain || !rootFlow.isCertain() ? Status.UNCERTAIN : Status.NONE);
        if (TRACE_LOG && result == Status.FOUND) {
            //打开调试功能，输出信息
            TheZenithMod.LOGGER.info("[Zenith] {} 的 {} 间接收尾链: {}", owner.name, method.name, chain);
        }
        return new TraceResult(result, goals, chain);
    }

    //检查一次调用，把一次调用转化成直接的方法调用（即lambda表达式转化为对应的方法调用），并返回能否找到收尾目标
    private Status checkCallSite(AbstractInsnNode inst, int depth
        , Set<String> visiting, Deque<String> path, int[] budget) {
        //是直接方法调用，直接检查
        if (inst instanceof MethodInsnNode call) {
            return checkCall(
                call.owner, call.name, call.desc, call.getOpcode(),
                depth, visiting, path, budget
            );
        }

        //剩下lambda表达式这类的调用，找出其中的方法调用
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) inst;
        boolean uncertain = false;
        for (Object argument : indy.bsmArgs) {
            if (!(argument instanceof Handle handle)) {
                //不是方法，检查下一个
                continue;
            }
            int opcode = ClassInfo.opcodeOf(handle);
            if (opcode < 0) {
                //不是方法调用，检查下一个
                continue;
            }
            Status status = checkCall(
                handle.getOwner(), handle.getName(), handle.getDesc(), opcode,
                depth, visiting, path, budget
            );
            if (status == Status.FOUND) {
                //找到了，可以确定
                return Status.FOUND;
            }
            if (status == Status.UNCERTAIN) {
                //结果不确定，标记
                uncertain = true;
            }
        }
        //还没找到，如果结果不确定，那就不确定，如果确定，那就是没有
        return uncertain ? Status.UNCERTAIN : Status.NONE;
    }

    //对方法调用进行检查和扫描，先检查当前的调用，失败了就从子类中找到同名方法调用进行检查，考虑同名方法的重写和覆盖
    private Status checkCall(String callOwner, String name, String desc
        , int opcode, int depth, Set<String> visiting, Deque<String> path, int[] budget) {

        if (depth > MAX_DEPTH) {
            //调用深度过高，直接结束扫描
            return Status.UNCERTAIN;
        }
        if ("<init>".equals(name) || "<clinit>".equals(name)) {
            //初始化方法，显然不是目标
            return Status.NONE;
        }

        //静态方法和父类方法
        if (opcode == Opcodes.INVOKESTATIC || opcode == Opcodes.INVOKESPECIAL) {
            if (isDirectTerminalMethod(callOwner, name, desc)) {
                //如果是直接的收尾方法，就记录路径并返回
                path.addLast(simpleName(callOwner) + "." + name);
                return Status.FOUND;
            }
            if (budget[0] >= MAX_METHODS) {
                //扫描的方法数超标了，不继续扫描
                return Status.UNCERTAIN;
            }
            return checkMethod(callOwner, name, desc, depth, visiting, path, budget);
        }
        if (opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKEINTERFACE) {
            //不是虚方法和接口方法
            return Status.NONE;
        }
        if (budget[0] >= MAX_METHODS) {
            //扫描的方法数超标了，不继续扫描
            return Status.UNCERTAIN;
        }

        //目前先标记为确定
        boolean uncertain = false;
        //检查方法
        Status declared = checkMethod(callOwner, name, desc, depth, visiting, path, budget);
        if (declared == Status.FOUND) {
            return Status.FOUND;
        }
        if (declared == Status.UNCERTAIN) {
            uncertain = true;
        }

        int checked = 0;
        //扫描这个类的全部直接子类
        for (String subclass : LivingClassFinder.directSubclassesOf(callOwner)) {
            if (checked++ >= MAX_SUBCLASSES) {
                uncertain = true;
                break;
            }
            //先获取类信息
            ClassInfo info = info(subclass);
            if (info == null) {
                //没有类信息就继续下一个，且结果不确定，进行标记
                uncertain = true;
                continue;
            }
            if (info.method(name, desc) == null) {
                //当前类没有这个方法
                continue;
            }
            //子类的结果，如果找到了，就可以覆盖之前的结果
            Status override = checkMethod(subclass, name, desc, depth, visiting, path, budget);
            if (override == Status.FOUND) {
                return Status.FOUND;
            }
            if (override == Status.UNCERTAIN) {
                uncertain = true;
            }
        }
        return uncertain ? Status.UNCERTAIN : Status.NONE;
    }

    //只检查一条具体的方法调用，不去管继承链或者虚方法
    private Status checkMethod(String owner, String name, String desc, int depth
        , Set<String> visiting, Deque<String> path, int[] budget) {
        if (depth > MAX_DEPTH || budget[0] >= MAX_METHODS) {
            return Status.UNCERTAIN;
        }

        //方法的标识符
        String key = owner + "." + name + desc;
        if (visiting.contains(key)) {
            //已经调用过这条方法，说明是自身递归，显然不是收尾点
            return Status.NONE;
        }

        //先找出方法调用
        ClassInfo.MethodInfo info = findMethod(owner, name, desc);
        if (info == null) {
            //没找到，不确定
            return Status.UNCERTAIN;
        }

        ++budget[0];
        path.addLast(simpleName(owner) + "." + name);
        boolean found = false;
        boolean uncertain = false;
        //把当前的节点标记为已到达
        visiting.add(key);
        try {
            //如果是收尾方法了，就可以判定能够找到
            if (info.directTerminal()) {
                found = true;
                return Status.FOUND;
            }

            //遍历方法内部的调用（边），先看看是否为直接收尾
            for (ClassInfo.CallSite call : info.calls()) {
                if ((call.opcode() == Opcodes.INVOKESTATIC || call.opcode() == Opcodes.INVOKESPECIAL)
                    && isDirectTerminalMethod(call.owner(), call.name(), call.desc())) {
                    path.addLast(simpleName(call.owner()) + "." + call.name());
                    found = true;
                    return Status.FOUND;
                }
            }

            int checked = 0;
            //没有直接收尾，所以只能递归进行搜索，由于这些只是原始调用信息，需要用checkCall找出真正的方法调用
            for (ClassInfo.CallSite call : info.calls()) {
                if (checked++ >= MAX_CALLS_PER_METHOD) {
                    uncertain = true;
                    break;
                }
                Status status = checkCall(
                    call.owner(), call.name(), call.desc(), call.opcode(),
                    depth + 1, visiting, path, budget
                );
                if (status == Status.FOUND) {
                    found = true;
                    return Status.FOUND;
                }
                if (status == Status.UNCERTAIN) {
                    uncertain = true;
                }
            }
            if (!info.certain()) {
                uncertain = true;
            }
            return uncertain ? Status.UNCERTAIN : Status.NONE;
        } finally {
            visiting.remove(key);
            if (!found) {
                path.removeLast();
            }
        }
    }

    private boolean isDirectTerminalMethod(String owner, String name, String desc) {
        ClassInfo.MethodInfo info = findMethod(owner, name, desc);
        return info != null && info.directTerminal();
    }

    //清除一遍缓存
    public void clearCache() {
        this.classCache.clear();
        this.runtimeClasses.clear();
    }

    /**
     * 从对应类中获取符合描述的方法信息
     * @param owner 要查找的类的类名
     * @param name 方法名
     * @param desc 方法描述
     * @return 找到的方法信息
     */
    private ClassInfo.MethodInfo findMethod(String owner, String name, String desc) {
        //先从类名得到类信息
        ClassInfo current = info(owner);
        //用于限制循环次数上限
        int guard = 0;
        while (current != null && guard++ < 64) {
            //尝试从当前类找方法
            ClassInfo.MethodInfo info = current.method(name, desc);
            if (info != null) {
                //找到了就返回
                return info;
            }
            if (current.superName == null) {
                //没找到且没有超类，找不到
                return null;
            }
            //继续从父类中查找
            current = info(current.superName);
        }
        return null;
    }

    /**
     * 获取类名对应的类信息
     */
    private ClassInfo info(String internalName) {
        //缓存了就直接取出来
        ClassInfo runtimeClass = this.runtimeClasses.get(internalName);
        if (runtimeClass != null) {
            return runtimeClass;
        }
        //没有缓存，需要加载一遍然后缓存
        return classCache.computeIfAbsent(internalName, this::loadInfo).orElse(null);
    }

    /**
     * 通过内部类名加载类的信息
     */
    private Optional<ClassInfo> loadInfo(String internalName) {
        byte[] bytes = readClass(internalName);
        if (bytes == null) {
            //读取失败，返回空
            return Optional.empty();
        }
        try {
            //解析字节码
            return Optional.of(ClassInfo.parse(bytes));
        } catch (Exception exception) {
            TheZenithMod.LOGGER.warn("[Zenith] 无法解析类索引: {}", internalName, exception);
            return Optional.empty();
        }
    }

    //读出类名对应的类的字节码
    private byte[] readClass(String internalName) {
        //文件名
        String resource = internalName + ".class";
        try {
            //把字节码文件读取为输入流
            InputStream stream = loader != null ? loader.getResourceAsStream(resource) : null;
            if (stream == null) {
                //通过系统类加载器进行尝试
                stream = ClassLoader.getSystemResourceAsStream(resource);
            }
            if (stream == null) {
                //获取失败
                if (TRACE_LOG) {
                    TheZenithMod.LOGGER.warn(
                        "[Zenith] 无法从 {} 读取类资源 {}",
                        this.loader == null ? "bootstrap loader" : this.loader.getClass().getName(),
                        resource
                    );
                }
                return null;
            }
            try (InputStream input = stream) {
                //直接读取全部字节，正好是字节码文件的内容
                return input.readAllBytes();
            }
        } catch (Exception exception) {
            TheZenithMod.LOGGER.warn("[Zenith] 无法读取类字节码: {}", internalName, exception);
            return null;
        }
    }

    private static String simpleName(String internalName) {
        int index = internalName.lastIndexOf('/');
        return index < 0 ? internalName : internalName.substring(index + 1);
    }
}
