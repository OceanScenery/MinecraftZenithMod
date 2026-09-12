package com.oceanscenery.zenith.zenith_class.capabilities;

import com.oceanscenery.zenith.registry.ZenithCapabilities;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZenithIdMarkSerializable implements ICapabilitySerializable<CompoundTag> {
    private final ZenithIdMark idMark=new ZenithIdMark();
    public final LazyOptional<IZenithIdMark> idMarkOptional=LazyOptional.of(()->idMark);
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap== ZenithCapabilities.ZENITH_ID_MARK){
            return idMarkOptional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return idMark.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        idMark.deserialize(nbt);
    }
}
