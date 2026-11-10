package com.oceanscenery.zenith.mixin_extension;

import net.minecraft.world.entity.Entity;

public interface IZenithEntityExtension {
    void zenith$remove();

    void zenith$unremove();

    boolean zenith$removed();

    static IZenithEntityExtension of(Entity entity) {
        return (IZenithEntityExtension) entity;
    }
}
