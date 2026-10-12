package com.oceanscenery.zenith.class_bytecode.common;

public final class HookNames {
    public static final String OWNER =
        "com/oceanscenery/zenith/class_bytecode/common/Hooks";

    public static final String GET_HEALTH =
        "modifyGetHealth";
    public static final String GET_HEALTH_DESCRIPTOR =
        "(FLnet/minecraft/world/entity/LivingEntity;)F";

    public static final String SET_HEALTH =
        "beforeSetHealth";
    public static final String SET_HEALTH_DESCRIPTOR =
        "(FLnet/minecraft/world/entity/LivingEntity;)V";

    public static final String IS_DEAD_OR_DYING =
        "modifyIsDeadOrDying";
    public static final String IS_DEAD_OR_DYING_DESCRIPTOR =
        "(ZLnet/minecraft/world/entity/LivingEntity;)Z";

    public static final String IS_ALIVE =
        "modifyIsAlive";
    public static final String IS_ALIVE_DESCRIPTOR =
        "(ZLnet/minecraft/world/entity/LivingEntity;)Z";

    private HookNames() {
    }
}
