package com.oceanscenery.zenith.zenith_class.capabilities;

import com.oceanscenery.zenith.registry.ZenithCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public class ZenithDamageMark implements IZenithDamageMark {
    private final HashMap<UUID, Float> map;

    public ZenithDamageMark(HashMap<UUID, Float> map) {
        this.map = map;
    }

    public ZenithDamageMark() {
        this(new HashMap<>());
    }

    @Override
    public HashMap<UUID, Float> getMap() {
        return this.map;
    }

    public static class Serializable implements ICapabilitySerializable<CompoundTag> {
        private ZenithDamageMark mark = new ZenithDamageMark();
        public final LazyOptional<ZenithDamageMark> optional = LazyOptional.of(() -> mark);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ZenithCapabilities.ZENITH_DAMAGE_MARK) {
                return this.optional.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            for (var entry : this.mark.map.entrySet()) {
                tag.putFloat(
                    entry.getKey().toString(),
                    entry.getValue()
                );
            }
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            this.mark.map.clear();
            for (String key : nbt.getAllKeys()) {
                float value = nbt.getFloat(key);
                try {
                    this.mark.map.put(
                        UUID.fromString(key),
                        value
                    );
                } catch (Exception e) {
                    continue;
                }
            }
        }
    }
}
