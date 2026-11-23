package com.oceanscenery.zenith.mixin_extension;

import net.minecraft.world.entity.Entity;

import java.util.UUID;

public interface IZenithEntityExtension {
    boolean zenith$isAllReturn();

    void zenith$setAllReturn(boolean enabled);

    boolean zenith$inRemoval();

    void zenith$setInRemoval(boolean value);

    UUID zenith$rawId();

    static boolean marked(Object value) {
        return value instanceof IZenithEntityExtension entity && entity.zenith$isAllReturn();
    }

    static IZenithEntityExtension of(Entity entity) {
        return (IZenithEntityExtension) entity;
    }
}
