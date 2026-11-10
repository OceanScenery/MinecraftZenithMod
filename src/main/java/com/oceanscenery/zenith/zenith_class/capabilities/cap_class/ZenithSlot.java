package com.oceanscenery.zenith.zenith_class.capabilities.cap_class;

import com.oceanscenery.zenith.registry.ZenithCapabilities;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithSlot;
import com.oceanscenery.zenith.zenith_class.saved_data.ZenithSlotData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

public class ZenithSlot implements IZenithSlot {
    public static final double DEFAULT_DISTANCE = 20.0D;
    public static final int DEFAULT_MODE = 0;
    public static final int MODE_COUNT = 3;

    private boolean valid = false;
    private boolean open = false;
    private double distance = DEFAULT_DISTANCE;
    private int mode = DEFAULT_MODE;
    private final Player player;

    private ItemStack shadow = ItemStack.EMPTY;

    public ZenithSlot(Player player) {
        this.player = Objects.requireNonNull(player);
    }

    @Override
    public boolean valid() {
        return this.valid;
    }

    @Override
    public boolean open() {
        return this.valid && this.open;
    }

    @Override
    public void setValid(boolean value) {
        this.valid = value;
        if (this.getOwner().isRemoved() || this.getOwner().level().isClientSide()) {
            return;
        }
        ZenithSlotData.fromCurrentServer().ifPresent(
            d -> d.setValid(player, value)
        );
    }

    @Override
    public void toggle(Function<Boolean, Boolean> change) {
        if (this.valid) {
            this.open = change.apply(this.open);
        }
    }

    @Override
    public void toggle() {
        if (valid) {
            this.open = !this.open;
        }
    }

    @Override
    public ItemStack get() {
        if (this.open()) {
            if (!this.shadow.isEmpty()) {
                return this.shadow;
            }
            ItemStack zenith = ZenithItems.ZENITH.get().instanceOf((int) this.distance, this.mode);

            this.shadow = zenith;
            return zenith;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public double getDistance() {
        return this.distance;
    }

    @Override
    public void setDistance(double distance) {
        int val = (int) distance;
        if (val <= 0 || val > 100 || val % 20 != 0) {
            val = 20;
        }
        this.distance = val;
        this.discardCache();
    }

    @Override
    public int getMode() {
        return this.mode;
    }

    @Override
    public void setMode(int mode) {
        this.mode = Math.floorMod(mode, MODE_COUNT);
        this.discardCache();
    }

    @Override
    public @NotNull Player getOwner() {
        return this.player;
    }

    @Override
    public void discardCache() {
        if (!this.shadow.isEmpty()) {
            this.shadow.setCount(0);
        }
    }

    public static class Serializable implements ICapabilitySerializable<CompoundTag> {
        private final ZenithSlot slot;
        public final LazyOptional<ZenithSlot> optional;

        public Serializable(Player owner) {
            this.slot = new ZenithSlot(owner);
            this.optional = LazyOptional.of(() -> this.slot);
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == ZenithCapabilities.ZENITH_SLOT ? this.optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("Valid", this.slot.valid());
            tag.putBoolean("Open", this.slot.open());
            tag.putDouble("Distance", this.slot.getDistance());
            tag.putInt("Mode", this.slot.getMode());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            try {
                this.slot.valid = nbt.getBoolean("Valid");
                this.slot.open = nbt.getBoolean("Open");
                this.slot.distance = nbt.contains("Distance") ? nbt.getDouble("Distance") : DEFAULT_DISTANCE;
                this.slot.mode = nbt.contains("Mode") ? Math.floorMod(nbt.getInt("Mode"), MODE_COUNT) : DEFAULT_MODE;
                this.slot.shadow = ItemStack.EMPTY;
            } catch (Exception ignored) {
                this.slot.valid = false;
                this.slot.open = false;
                this.slot.distance = DEFAULT_DISTANCE;
                this.slot.mode = DEFAULT_MODE;
            }
        }
    }
}
