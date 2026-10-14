package com.oceanscenery.zenith.zenith_class.capabilities;

import com.oceanscenery.zenith.registry.ZenithCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HitByZenithMark implements IHitByZenithMark {
    private long gameTime;
    private boolean mark;

    public HitByZenithMark(long gameTime, boolean mark) {
        this.gameTime = gameTime;
        this.mark = mark;
    }

    @Override
    public long gameTime() {
        return this.gameTime;
    }

    @Override
    public boolean valid(net.minecraft.world.level.Level level) {
        long c = level.getGameTime() - this.gameTime;
        if (c < 0 || c > 20) {
            return false;
        }
        return this.mark;
    }

    @Override
    public boolean hit() {
        return this.mark;
    }

    @Override
    public void set(long gameTime, boolean hit) {
        this.gameTime = gameTime;
        this.mark = hit;
    }

    public static class Serializable implements ICapabilitySerializable<CompoundTag> {
        private HitByZenithMark mark;
        public final LazyOptional<HitByZenithMark> optional;

        public Serializable(HitByZenithMark mark) {
            this.mark = mark;
            this.optional = LazyOptional.of(() -> mark);
        }

        public Serializable() {
            this(new HitByZenithMark(0, false));
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ZenithCapabilities.HIT_BY_ZENITH_MARK) {
                return optional.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Hit", this.mark.mark);
            tag.putLong("GameTime", this.mark.gameTime);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            long t = nbt.getLong("GameTime");
            boolean flag = nbt.getBoolean("Hit");
            this.mark.mark = flag;
            this.mark.gameTime = t;
        }
    }
}
