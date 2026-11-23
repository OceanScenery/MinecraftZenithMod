package com.oceanscenery.agent.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Opcodes.*;

public final class LivingTotalPatcher {
    private static final String ENTITY = "net/minecraft/world/entity/Entity";
    private static final String EXTENSION = "com/oceanscenery/zenith/mixin_extension/IZenithEntityExtension";
    private static final String MARK_HOOK = "com/oceanscenery/zenith/hooks/EntityMarkHook";
    //要捕获的潜在异常
    private static final String[] EXCEPTIONS = {
        "java/lang/NullPointerException", "java/lang/IllegalArgumentException", "java/util/NoSuchElementException",
        "java/lang/IllegalStateException"
    };

    //用于保存一个类的父类，实现的接口以及声明的方法
    //methods为methodName->access的映射
    private record Header(String parent, List<String> interfaces, Map<String, Integer> methods) {
    }

    //特殊实例，用于表明没有成功读取有效的类信息
    private static final Header MISSING = new Header(null, List.of(), Map.of());
    private static final Map<ClassLoader, Map<String, Header>> HEADERS = new WeakHashMap<>();

    private LivingTotalPatcher() {
    }

    /**
     * 用于找到一个类的header
     * @param name 类的内部名
     * @param loader 类加载器
     * @return 类的Header对象，失败则返回MISSING
     */
    private static Header header(String name, ClassLoader loader) {
        Map<String, Header> cache;
        synchronized (HEADERS) {
            cache = HEADERS.computeIfAbsent(loader, ignored -> new ConcurrentHashMap<>());
        }
        return cache.computeIfAbsent(name, key -> {
            try (InputStream in = loader == null ? ClassLoader.getSystemResourceAsStream(key + ".class")
                : loader.getResourceAsStream(key + ".class")) {
                if (in == null) {
                    return MISSING;
                }
                ClassNode node = new ClassNode();
                new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                Map<String, Integer> methods = new HashMap<>();
                //加入方法信息
                for (MethodNode m : node.methods) {
                    methods.put(m.name + m.desc, m.access);
                }
                return new Header(node.superName, List.copyOf(node.interfaces), methods);
            } catch (IOException e) {
                return MISSING;
            }
        });
    }

    //检查类是否为Entity本身或其子类
    public static boolean isEntity(ClassNode node, ClassLoader loader) {
        if (ENTITY.equals(node.name)) {
            return true;
        }
        for (String n = node.superName; n != null && !"java/lang/Object".equals(n); n = header(n, loader).parent) {
            if (ENTITY.equals(n)) {
                return true;
            }
        }
        return false;
    }

    //检查一个类的某个方法是否有有效（有方法体）的父类方法
    private static boolean hasSuper(ClassNode owner, MethodNode m, ClassLoader loader) {
        if ((m.access & ACC_PRIVATE) != 0) {
            return false;
        }
        String key = m.name + m.desc;
        Set<String> interfaces = new LinkedHashSet<>();
        for (String n = owner.superName; n != null; ) {
            Header h = header(n, loader);
            if (h == MISSING) {
                //无效信息，保守起见返回false
                return false;
            }
            Integer access = h.methods.get(key);
            if (access != null) {
                //如果在父类找到了同名方法
                //无方法体或者private和static方法一定无父类方法
                if ((access & (ACC_PRIVATE | ACC_STATIC | ACC_ABSTRACT)) != 0) {
                    return false;
                }
                //public和protected一定可以调用父类方法，默认权限修饰符则需要看是否同包
                return (access & (ACC_PUBLIC | ACC_PROTECTED)) != 0 ||
                    n.substring(0, n.lastIndexOf('/') + 1).equals(owner.name.substring(0, owner.name.lastIndexOf('/') + 1));
            }
            //沿路收集所有实现的接口
            interfaces.addAll(h.interfaces);
            n = h.parent;
        }
        //声明了目标方法的接口集合
        Set<String> declarations = new HashSet<>();
        //栈
        Deque<String> pending = new ArrayDeque<>(interfaces);
        //已经浏览过的接口
        Set<String> visited = new HashSet<>();
        while (!pending.isEmpty()) {
            String name = pending.pop();
            if (!visited.add(name)) {
                //已经浏览过，所以不重复浏览
                continue;
            }
            //找到接口的Header对象
            Header h = header(name, loader);
            if (h == MISSING) {
                return false;
            }
            //向上遍历接口的同名方法
            Integer access = h.methods.get(key);
            if (access != null && (access & (ACC_PRIVATE | ACC_STATIC)) == 0) {
                declarations.add(name);
            }
            pending.addAll(h.interfaces);
        }
        Set<String> mostSpecific = new HashSet<>(declarations);
        visited.clear();
        for (String name : declarations) {
            //所有要处理接口的所有父类接口，可能与已有接口部分重合
            pending.addAll(header(name, loader).interfaces);
        }
        while (!pending.isEmpty()) {
            //将所有父类接口移除，以达到只调用直接父类接口方法的效果
            String name = pending.pop();
            if (!visited.add(name)) {
                continue;
            }
            mostSpecific.remove(name);
            pending.addAll(header(name, loader).interfaces);
        }
        //排除掉接口根本没有实现的可能性
        //排除掉有两个可调用的接口默认方法的可能性
        return mostSpecific.stream().filter(n -> (header(n, loader).methods.get(key) & ACC_ABSTRACT) == 0).count() == 1;
    }

    //默认返回false或0或无返回值的原版方法
    private static final Set<String> VANILLA_ZERO = Set.of(
        "getHealth()F", "m_21223_()F", "isAlive()Z", "m_6084_()Z", "isPickable()Z", "m_6087_()Z",
        "isAttackable()Z", "m_6097_()Z", "shouldBeSaved()Z", "m_142391_()Z",
        "save(Lnet/minecraft/nbt/CompoundTag;)Z", "m_20223_(Lnet/minecraft/nbt/CompoundTag;)Z",
        "saveAsPassenger(Lnet/minecraft/nbt/CompoundTag;)Z", "m_20086_(Lnet/minecraft/nbt/CompoundTag;)Z",
        "shouldRender(DDD)Z", "m_6000_(DDD)Z", "shouldRenderAtSqrDistance(D)Z", "m_6783_(D)Z",
        "tick()V", "m_8119_()V", "aiStep()V", "m_8107_()V", "baseTick()V", "m_6075_()V",
        "rideTick()V", "m_6083_()V", "tickDeath()V", "m_6153_()V"
    );
    //默认要返回true的原版方法
    private static final Set<String> VANILLA_TRUE = Set.of(
        "isDeadOrDying()Z", "m_21224_()Z", "isRemoved()Z", "m_213877_()Z"
    );

    public static boolean transform(ClassNode owner, ClassLoader loader, boolean vanilla, boolean entity) {
        boolean changed = false;
        for (MethodNode m : owner.methods) {
            //不改变构造方法和类初始化方法
            if (m.name.startsWith("<") || (m.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0 || m.instructions.size() == 0) {
                continue;
            }
            //用于判断的方法不能进行修改
            if (("zenith$isAllReturn".equals(m.name) && "()Z".equals(m.desc))
                || ("zenith$setAllReturn".equals(m.name) && "(Z)V".equals(m.desc))) {
                continue;
            }
            //额外排除这两个方法
            if ("equals".equals(m.name) || ("hashCode").equals(m.name)) {
                continue;
            }
            String key = m.name + m.desc;
            //返回值是否为true（反之则为0）
            boolean trueResult = vanilla && VANILLA_TRUE.contains(key);
            //对于原版的Entity类以外的方法不做处理
            if (vanilla && (!entity || (m.access & ACC_STATIC) != 0 || (!trueResult && !VANILLA_ZERO.contains(key)))) {
                continue;
            }
            //对于已经处理过的类不做处理
            if (alreadyPatched(m, vanilla)) {
                continue;
            }
            //当前方法的返回值
            Type result = Type.getReturnType(m.desc);
            //当前方法的参数类型
            Type[] args = Type.getArgumentTypes(m.desc);
            //当前方法不为静态方法
            boolean instance = (m.access & ACC_STATIC) == 0;
            //是否在开头作标记检查（实体对象调用成员方法，需要检查自身的移除标记）
            boolean checkSelf = instance && entity;
            //是否要调用父类方法
            boolean callSuper = !vanilla && checkSelf && hasSuper(owner, m, loader);
            //在开头，变量检查(对于Object参数,需要检查它是否为标记的实体,如果是的话就直接返回)和父类方法处的标签(?)
            LabelNode start = new LabelNode(), fallback = new LabelNode(), parent = new LabelNode();
            InsnList head = new InsnList();
            head.add(start);
            //如果为true，代表有可能要直接返回默认值而不是调用父类方法（即参数合法性检查优先于调用父类方法）
            boolean usesFallback = false;
            if (!vanilla) {
                int slot = instance ? 1 : 0;
                for (Type arg : args) {
                    if (arg.getSort() == Type.OBJECT) {
                        //如果有类对象就需要进行合法性检查
                        branchIfMarked(head, slot, fallback);
                        usesFallback = true;
                    }
                    //检查下一个
                    slot += arg.getSize();
                }
            }
            if (checkSelf) {
                branchIfMarked(head, 0, callSuper ? parent : fallback);
                usesFallback |= !callSuper;
            }
            //把语句插在开头
            m.instructions.insert(head);

            if (callSuper) {
                m.instructions.add(parent);
                List<Object> locals = new ArrayList<>();
                locals.add(owner.name);
                for (Type arg : args) {
                    locals.add(frameType(arg));
                }
                //label
                //stackframe
                //load ...
                //invokespecial
                //return
                m.instructions.add(new FrameNode(F_FULL, locals.size(), locals.toArray(), 0, null));
                m.instructions.add(new VarInsnNode(ALOAD, 0));
                int slot = 1;
                for (Type arg : args) {
                    m.instructions.add(new VarInsnNode(arg.getOpcode(ILOAD), slot));
                    slot += arg.getSize();
                }
                m.instructions.add(new MethodInsnNode(INVOKESPECIAL, owner.superName, m.name, m.desc, false));
                m.instructions.add(new InsnNode(result.getOpcode(IRETURN)));
            }
            if (!vanilla) {
                //try types
                //catch
                //label handler
                //stackframe
                //invokestatic ...
                //
                LabelNode handler = new LabelNode();
                m.instructions.add(handler);
                m.instructions.add(new FrameNode(F_FULL, 0, null, 1, new Object[]{"java/lang/RuntimeException"}));
                m.instructions.add(new MethodInsnNode(INVOKESTATIC, "com/oceanscenery/zenith/util/ConfigUtil",
                    "rethrowUnlessRemove", "(Ljava/lang/RuntimeException;)V", false));
                for (String type : EXCEPTIONS) {
                    m.tryCatchBlocks.add(new TryCatchBlockNode(start, handler, handler, type));
                }
            }
            if (usesFallback) {
                //label fallback
                //stackframe
                m.instructions.add(fallback);
                m.instructions.add(new FrameNode(F_FULL, 0, null, 0, null));
            }
            //在最后加入返回语句
            appendReturn(m.instructions, result, trueResult);
            changed = true;
        }
        return changed;
    }

    /**
     * 用于插入一段条件跳转代码
     * @param out 用于输出指令的列表
     * @param slot 局部变量的序号
     * @param target 跳转目标
     */
    private static void branchIfMarked(InsnList out, int slot, LabelNode target) {
        out.add(new VarInsnNode(ALOAD, slot));
        out.add(new MethodInsnNode(INVOKESTATIC, MARK_HOOK, "marked", "(Ljava/lang/Object;)Z", false));
        out.add(new JumpInsnNode(IFNE, target));
    }

    //获取类型对应的栈帧类型
    private static Object frameType(Type t) {
        return switch (t.getSort()) {
            case Type.OBJECT, Type.ARRAY -> t.getInternalName();
            case Type.LONG -> LONG;
            case Type.DOUBLE -> DOUBLE;
            case Type.FLOAT -> FLOAT;
            default -> INTEGER;
        };
    }

    //在方法最后加入返回语句
    private static void appendReturn(InsnList out, Type type, boolean trueResult) {
        int constant = switch (type.getSort()) {
            case Type.VOID -> -1;
            case Type.OBJECT, Type.ARRAY -> ACONST_NULL;
            case Type.FLOAT -> FCONST_0;
            case Type.LONG -> LCONST_0;
            case Type.DOUBLE -> DCONST_0;
            default -> trueResult ? ICONST_1 : ICONST_0;
        };
        if (constant >= 0) {
            out.add(new InsnNode(constant));
        }
        out.add(new InsnNode(type.getOpcode(IRETURN)));
    }

    //是否已经处理过
    private static boolean alreadyPatched(MethodNode m, boolean vanilla) {
        if (!vanilla) {
            //try块数量不达标
            int size = m.tryCatchBlocks.size();
            if (size < EXCEPTIONS.length) {
                return false;
            }
            TryCatchBlockNode last = m.tryCatchBlocks.get(size - 1);
            for (int i = 0; i < EXCEPTIONS.length; i++) {
                TryCatchBlockNode block = m.tryCatchBlocks.get(size - EXCEPTIONS.length + i);
                //检查是否为添加的try块
                if (block.start != last.start || block.end != block.handler || block.handler != last.handler
                    || !EXCEPTIONS[i].equals(block.type)) {
                    return false;
                }
            }
            return true;
        }
        //检查开头的插入是否为相关插入
        AbstractInsnNode n = m.instructions.getFirst();
        while (n != null && n.getOpcode() < 0) {
            n = n.getNext();
        }
        return n instanceof VarInsnNode v
            && v.getOpcode() == ALOAD && v.var == 0
            && n.getNext() instanceof MethodInsnNode call
            && call.getOpcode() == INVOKESTATIC
            && (MARK_HOOK.equals(call.owner) || EXTENSION.equals(call.owner))
            && "marked".equals(call.name) && "(Ljava/lang/Object;)Z".equals(call.desc);
    }
}
