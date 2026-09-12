package com.oceanscenery.zenith.zenith_class.capabilities;

import com.oceanscenery.zenith.registry.ZenithCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZenithUuidMarkSerializable implements ICapabilitySerializable<CompoundTag> {
    private final ZenithUuidMark uuidMark=new ZenithUuidMark();
    public final LazyOptional<ZenithUuidMark> optional=LazyOptional.of(()->uuidMark);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap== ZenithCapabilities.ZENITH_UUID_MARK){
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return uuidMark.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        uuidMark.deserialize(nbt);
    }
}
