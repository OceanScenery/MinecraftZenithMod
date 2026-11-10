package com.oceanscenery.zenith.mod_class.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record ZenithIdMark(Set<String> ids) {
    public static final Codec<ZenithIdMark> CODEC = Codec.STRING.listOf().<ZenithIdMark>xmap(
        strList -> new ZenithIdMark(new HashSet<>(strList.stream().filter(ZenithIdMark::validType).toList())),
        mark -> mark.ids.stream().filter(ZenithIdMark::validType).toList()
    );

    public static boolean validType(String type) {
        try {
            ResourceLocation res = ResourceLocation.parse(type);
            if (BuiltInRegistries.ENTITY_TYPE.containsKey(res)) {
                return true;
            }
            return false;
        } catch (Throwable throwable) {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ZenithIdMark(Set<String> ids1))) {
            return false;
        }
        return Objects.equals(ids, ids1);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ids);
    }
}
