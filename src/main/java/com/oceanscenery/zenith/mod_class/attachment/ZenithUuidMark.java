package com.oceanscenery.zenith.mod_class.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.UUIDUtil;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ZenithUuidMark(Set<UUID> uuids) {
    public static final Codec<ZenithUuidMark> CODEC = UUIDUtil.STRING_CODEC.listOf().xmap(
        uuidList -> new ZenithUuidMark(new HashSet<>(uuidList)),
        mark -> mark.uuids().stream().toList()
    );

    public static final MapCodec<ZenithUuidMark> MAP_CODEC = CODEC.fieldOf("uuids");

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ZenithUuidMark(Set<UUID> uuids1))) {
            return false;
        }
        return Objects.equals(uuids, uuids1);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuids);
    }
}
