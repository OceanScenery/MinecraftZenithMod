package com.oceanscenery.zenith.zenith_class.capabilities.cap_interface;

import net.minecraft.world.level.Level;

public interface IZenithHitMark {
    long startTime();

    long endTime();

    boolean valid(Level level);

    boolean hit();

    void set(long startTime,long endTime,boolean hit);
}
