package com.oceanscenery.zenith.zenith_class.capabilities.cap_interface;

import net.minecraft.world.entity.Entity;

public interface IZenithHealthMark {
    boolean valid(Entity entity);

    float value();

    boolean rawValid();

    void set(float value,boolean valid);
}
