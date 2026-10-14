package com.oceanscenery.zenith.bootstrap;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class LivingEntityTargetIndex {
    public static final String LIVING_ENTITY =
        "net/minecraft/world/entity/LivingEntity";

    public static Set<String> findTargets() {
        Map<String, String> parents = getStringMap();

        Set<String> result = new HashSet<>();
        result.add(LIVING_ENTITY);

        for (String name : parents.keySet()) {
            String current = name;
            while (current != null) {
                if (LIVING_ENTITY.equals(current)) {
                    result.add(name);
                    break;
                }
                current = parents.get(current);
            }
        }
        return result;
    }

    public static Set<String> baseTargets() {
        return Set.of(LIVING_ENTITY);
    }

    private static @NotNull Map<String, String> getStringMap() {
        Map<String, String> parents = new HashMap<>();

        for (ModFileInfo fileInfo
            : FMLLoader.getLoadingModList().getModFiles()) {
            ModFileScanData data = fileInfo.getFile().getScanResult();
            if (data == null) {
                continue;
            }

            for (ModFileScanData.ClassData classData : data.getClasses()) {
                parents.put(
                    classData.clazz().getInternalName(),
                    classData.parent() == null
                        ? null
                        : classData.parent().getInternalName()
                );
            }
        }
        return parents;
    }
}
