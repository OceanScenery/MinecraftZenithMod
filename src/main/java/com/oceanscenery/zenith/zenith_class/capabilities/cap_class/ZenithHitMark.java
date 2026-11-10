package com.oceanscenery.zenith.zenith_class.capabilities.cap_class;

import com.oceanscenery.zenith.registry.ZenithCapabilities;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithHitMark;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZenithHitMark implements IZenithHitMark {
    private long startTime;
    private long endTime;
    private boolean mark;

    public ZenithHitMark(long startTime, long endTime, boolean mark) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.mark = mark;
    }

    @Override
    public long startTime() {
        return this.startTime;
    }

    @Override
    public long endTime() {
        return this.endTime;
    }

    @Override
    public boolean valid(net.minecraft.world.level.Level level) {
        long c = level.getGameTime();
        if (c < this.startTime || c > this.endTime) {
            return false;
        }
        return this.mark;
    }

    @Override
    public boolean hit() {
        return this.mark;
    }

    @Override
    public void set(long startTime, long endTime, boolean hit) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.mark = hit;
    }

    public static class Serializable implements ICapabilitySerializable<CompoundTag> {
        private ZenithHitMark mark;
        public final LazyOptional<ZenithHitMark> optional;

        public Serializable(ZenithHitMark mark) {
            this.mark = mark;
            this.optional = LazyOptional.of(() -> mark);
        }

        public Serializable() {
            this(new ZenithHitMark(0, 0, false));
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
            tag.putLong("StartTime", this.mark.startTime);
            tag.putLong("EndTime", this.mark.endTime);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            try {
                this.mark.mark = nbt.getBoolean("Hit");
                this.mark.startTime = nbt.getLong("StartTime");
                this.mark.endTime = nbt.getLong("EndTime");
            } catch (Exception e) {
                this.mark.mark = false;
                this.mark.startTime = 0;
                this.mark.endTime = 0;
            }
        }
    }
}
