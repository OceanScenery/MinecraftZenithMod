package com.oceanscenery.agent.util;

import com.oceanscenery.agent.util.MethodData.ParamType;

public interface MethodConst {
    String LIVING_ENTITY = "net/minecraft/world/entity/LivingEntity";
    String ENTITY = "net/minecraft/world/entity/Entity";
    String REMOVAL_REASON = "net/minecraft/world/entity/Entity$RemovalReason";
    String DAMAGE_SOURCE = "net/minecraft/world/damagesource/DamageSource";
    String SERVER_LEVEL = "net.minecraft.server.level.ServerLevel".replace('.','/');
    MethodData TICK_DEATH = MethodData.of(
        LIVING_ENTITY,"tickDeath","m_6153_",
        ParamType.of(ParamType.VOID)
    );
    MethodData REMOVE = MethodData.of(
        ENTITY,"remove","m_142687_",
        ParamType.of(ParamType.ofClass(REMOVAL_REASON), ParamType.VOID)
    );
    MethodData SET_REMOVED = MethodData.of(
        ENTITY,"setRemoved","m_142467_",
        ParamType.of(ParamType.ofClass(REMOVAL_REASON),ParamType.VOID)
    );
    MethodData KILL = MethodData.of(
        ENTITY,"kill","m_6074_",
        ParamType.of(ParamType.VOID)
    );
    MethodData DISCARD = MethodData.of(
        ENTITY,"discard","m_146870_",
        ParamType.of(ParamType.VOID)
    );
    MethodData DIE = MethodData.of(
        LIVING_ENTITY,"die","m_6667_",
        ParamType.of(ParamType.ofClass(DAMAGE_SOURCE),ParamType.VOID)
    );
    MethodData DROP_LOOT = MethodData.of(
        LIVING_ENTITY,"dropAllDeathLoot","m_6668_",
        ParamType.of(ParamType.ofClass(SERVER_LEVEL),ParamType.ofClass(DAMAGE_SOURCE),ParamType.VOID)
    );
}
