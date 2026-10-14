package com.oceanscenery.zenith.zenith_class.capabilities;

import net.minecraft.world.level.Level;

public interface IZenithHealthMark {
    long gameTime();

    boolean valid(Level level);

    float value();

    boolean rawValid();

    void set(float value,long gameTime,boolean valid);
}
