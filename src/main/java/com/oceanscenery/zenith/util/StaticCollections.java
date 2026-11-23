package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.event.ServerTickTask;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StaticCollections {
    public static final Map<LivingEntity, DamageHandler.VictimRecord> checkedVictims = new ConcurrentHashMap<>();
    public static final Queue<ServerTickTask> taskList = new ArrayDeque<>();
    public static final ThreadLocal<Set<Entity>> REMOVE_MARK
        = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));
}
