package com.oceanscenery.zenith.zenith_class.capabilities;

import net.minecraft.world.level.Level;

public interface IHitByZenithMark {
    long gameTime();

    boolean valid(Level level);

    boolean hit();

    void set(long gameTime,boolean hit);
}
