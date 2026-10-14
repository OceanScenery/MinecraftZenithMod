package com.oceanscenery.zenith.zenith_class.capabilities;

import com.oceanscenery.zenith.registry.ZenithCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZenithHealthMark implements IZenithHealthMark {
    private long gameTime;
    private boolean valid;
    private float value;

    public ZenithHealthMark(float value, boolean valid, long time) {
        this.value = value;
        this.valid = valid;
        this.gameTime = time;
    }

    @Override
    public long gameTime() {
        return this.gameTime;
    }

    @Override
    public boolean valid(Level level) {
        long c = level.getGameTime() - this.gameTime;
        if (c < 0 || c > 20) {
            return false;
        }
        return this.valid;
    }

    @Override
    public float value() {
        return this.value;
    }

    @Override
    public boolean rawValid() {
        return this.valid;
    }

    @Override
    public void set(float value, long gameTime, boolean valid) {
        this.value = value;
        this.gameTime = gameTime;
        this.valid = valid;
    }

    public static class Serializable implements ICapabilitySerializable<CompoundTag> {
        private ZenithHealthMark mark;
        public final LazyOptional<ZenithHealthMark> optional;

        public Serializable(ZenithHealthMark mark) {
            this.mark = mark;
            this.optional = LazyOptional.of(() -> mark);
        }

        public Serializable() {
            this(new ZenithHealthMark(0f, false, 0));
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ZenithCapabilities.ZENITH_HEALTH_MARK) {
                return optional.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("GameTime", this.mark.gameTime);
            tag.putBoolean("Valid", this.mark.valid);
            tag.putFloat("Health", this.mark.value);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            long time = nbt.getLong("GameTime");
            boolean flag = nbt.getBoolean("Valid");
            float value = nbt.getFloat("Health");
            this.mark.gameTime = time;
            this.mark.valid = flag;
            this.mark.value = value;
        }
    }
}
