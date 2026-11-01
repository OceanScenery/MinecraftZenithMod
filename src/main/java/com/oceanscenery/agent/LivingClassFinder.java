package com.oceanscenery.agent;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;

import java.util.*;

public final class LivingClassFinder {
    public static final String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    public static final String ENTITY = "net/minecraft/world/entity/Entity";

    private static volatile Map<String, String> parents;
    private static volatile Map<String, List<String>> children;

    private LivingClassFinder() {
    }

    //获取所有要进行修改的类名
    public static Set<String> findTargets() {
        Map<String, String> parents = parents();

        Set<String> result = new HashSet<>();
        //这两个类肯定要处理
        result.add(LIVING_ENTITY);
        result.add(ENTITY);

        for (String name : parents.keySet()) { //所有有父类的类名
            String current = name;
            Set<String> visited = new HashSet<>();
            while (current != null && visited.add(current)) {
                //一路向2上寻找，如果找到了LivingEntity类，说明是其子类
                if (LIVING_ENTITY.equals(current)) {
                    result.add(name);
                    break;
                }
                current = parents.get(current);
            }
        }
        return result;
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
