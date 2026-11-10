package com.oceanscenery.zenith.mixin_extension;

import net.minecraft.world.entity.LivingEntity;

public interface IZenithLivingEntityExtension {
    int zenith$deathTime();

    void zenith$setDeathTime(int value);

    boolean zenith$deadMark();

    void zenith$setDead(boolean value);

    static IZenithLivingEntityExtension of(LivingEntity entity) {
        return (IZenithLivingEntityExtension) entity;
    }
}
