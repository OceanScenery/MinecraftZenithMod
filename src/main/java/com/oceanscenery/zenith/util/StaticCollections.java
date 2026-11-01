package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.event.ServerTickTask;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StaticCollections {
    public static final Map<LivingEntity, DamageHandler.VictimRecord> checkedVictims = new ConcurrentHashMap<>();
    public static final Queue<ServerTickTask> taskList = new ArrayDeque<>();
    public static final Map<LivingEntity, Integer> tickDeathList = new ConcurrentHashMap<>();
    public static final Set<LivingEntity> removed = Collections.newSetFromMap(new ConcurrentHashMap<>());
}
