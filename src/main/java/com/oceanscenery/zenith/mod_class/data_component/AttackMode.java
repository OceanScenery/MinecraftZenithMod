package com.oceanscenery.zenith.mod_class.data_component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Map;
import java.util.Optional;

public enum AttackMode {
    LIVING_ENTITY("living_entity"),
    ATTACKABLE_ENTITY("attackable_entity"),
    ALL("all");

    public final String entityMode;

    public static final Map<String, AttackMode> NAME = Map.of(
        "living_entity", LIVING_ENTITY,
        "attackable_entity", ATTACKABLE_ENTITY,
        "all", ALL
    );

    public static final Codec<AttackMode> CODEC = Codec.STRING.xmap(
        AttackMode::fromString,
        AttackMode::entityMode
    );

    public static final MapCodec<AttackMode> MAP_CODEC = CODEC.fieldOf("mode");

    public static final StreamCodec<ByteBuf, AttackMode> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
        AttackMode::fromString,
        AttackMode::entityMode
    );

    AttackMode(String entityMode) {
        this.entityMode = entityMode;
    }

    public static AttackMode fromString(String name) {
        return Optional.ofNullable(NAME.get(name)).orElse(LIVING_ENTITY);
    }

    public String entityMode() {
        return this.entityMode;
    }

    public AttackMode next() {
        switch (this) {
            case LIVING_ENTITY -> {
                return ATTACKABLE_ENTITY;
            }
            case ATTACKABLE_ENTITY -> {
                return ALL;
            }
            default -> {
                return LIVING_ENTITY;
            }
        }
    }
}
