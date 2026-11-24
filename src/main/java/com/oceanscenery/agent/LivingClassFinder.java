package com.oceanscenery.agent;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public final class LivingClassFinder {
    public static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    public static final String ENTITY = "net/minecraft/world/entity/Entity";
    //找到所有Entity类的父类
    private static final Set<String> ENTITY_SUPERTYPES = entitySupertypes();

    private static Set<String> entitySupertypes() {
        Set<String> names = new HashSet<>();
        Deque<Class<?>> pending = new ArrayDeque<>();
        pending.add(net.minecraft.world.entity.Entity.class);
        while (!pending.isEmpty()) {
            //加入Entity父类及所有的实现接口
            Class<?> type = pending.removeFirst();
            if (!names.add(type.getName().replace('.', '/'))) {
                continue;
            }
            if (type.getSuperclass() != null) {
                pending.add(type.getSuperclass());
            }
            Collections.addAll(pending, type.getInterfaces());
        }
        return Set.copyOf(names);
    }

    public static boolean isEntitySupertype(String internalName) {
        return ENTITY_SUPERTYPES.contains(internalName);
    }

    private static volatile Map<String, String> parents;
    private static volatile Map<String, List<String>> children;

    private LivingClassFinder() {
    }

    //获取所有要进行修改的类名
    public static Set<String> findTargets() {
        Map<String, String> parents = parents();

        Set<String> result = new HashSet<>();
        result.add(LIVING_ENTITY);
        Set<String> nonLiving = new HashSet<>();
        nonLiving.add(ENTITY);
        Map<String, String> resolved = new HashMap<>(parents);
        Set<String> visited = new HashSet<>();
        for (String name : parents.keySet()) {
            if (result.contains(name) || nonLiving.contains(name)) {
                continue;
            }
            visited.clear();
            String current = name;
            while (current != null && !result.contains(current) && !nonLiving.contains(current) && visited.add(current)) {
                if (!resolved.containsKey(current)) {
                    resolved.put(current, readParent(current));
                }
                current = resolved.get(current);
            }
            if (current != null && result.contains(current)) {
                result.addAll(visited);
            } else {
                nonLiving.addAll(visited);
            }
        }
        result.add(ENTITY);
        return result;
    }

    private static String readParent(String internalName) {
        if (internalName.startsWith("java/")) {
            return null;
        }
        try (InputStream input = LivingClassFinder.class.getClassLoader().getResourceAsStream(internalName + ".class")) {
            return input == null ? null : new ClassReader(input).getSuperName();
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * 获取一个map，这个map是从一个类名到它父类类名的映射
     */
    public static Map<String, String> parents() {
        //先获取缓存
        Map<String, String> current = parents;
        if (current == null) {
            //如果缓存不存在，就加锁，然后初始化缓存
            synchronized (LivingClassFinder.class) {
                //再次确认，防止拿锁期间初始化
                current = parents;
                if (current == null) {
                    //加载
                    current = scanParents();
                    parents = current;
                }
            }
        }
        return current;
    }

    //获取一个类的直接子类
    public static List<String> directSubclassesOf(String internalName) {
        //与parent对应的从父类到子类的映射
        Map<String, List<String>> current = children;
        if (current == null) {
            synchronized (LivingClassFinder.class) {
                current = children;
                if (current == null) {
                    Map<String, List<String>> built = new HashMap<>();
                    //从parent反向假如所有映射
                    parents().forEach((child, parent) -> {
                        if (parent != null) {
                            built.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(child);
                        }
                    });
                    children = current = built;
                }
            }
        }
        return current.getOrDefault(internalName, List.of());
    }

    public static synchronized void clearCaches() {
        parents = null;
        children = null;
    }

    private static Map<String, String> scanParents() {
        Map<String, String> result = new HashMap<>();
        ModList modList = ModList.get();
        if (modList == null) {
            //没有模组加载，直接返回空列表
            return result;
        }
        for (ModFileScanData data : modList.getAllScanData()) {
            for (ModFileScanData.ClassData classData : data.getClasses()) {
                //获取所有模组信息中的所有类的信息，并假如它的类名到它父类类名的映射
                result.put(
                    classData.clazz().getInternalName(),
                    classData.parent() == null ? null : classData.parent().getInternalName()
                );
            }
        }
        return result;
    }
}
