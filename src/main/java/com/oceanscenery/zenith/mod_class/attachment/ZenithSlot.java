package com.oceanscenery.zenith.mod_class.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.registry.ZenithItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class ZenithSlot {
    private final boolean valid;
    private final boolean open;
    private final double distance;
    private final AttackMode mode;

    private ItemStack shadow = ItemStack.EMPTY;

    public static final Codec<ZenithSlot> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.BOOL.fieldOf("valid").forGetter(ZenithSlot::valid),
            Codec.BOOL.fieldOf("open").forGetter(ZenithSlot::open),
            Codec.DOUBLE.fieldOf("distance").forGetter(ZenithSlot::distance),
            AttackMode.CODEC.fieldOf("mode").forGetter(ZenithSlot::mode)
        ).apply(
            instance, ZenithSlot::new
        )
    );

    public static final StreamCodec<ByteBuf, ZenithSlot> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, ZenithSlot::valid,
        ByteBufCodecs.BOOL, ZenithSlot::open,
        ByteBufCodecs.DOUBLE, ZenithSlot::distance,
        AttackMode.STREAM_CODEC, ZenithSlot::mode,
        ZenithSlot::new
    );

    public ZenithSlot(boolean valid, boolean open, double distance, AttackMode mode) {
        this.valid = valid;
        this.open = open;
        this.distance = ensureDistanceLegal(distance);
        this.mode = mode;
    }

    public boolean valid() {
        return this.valid;
    }

    public boolean open() {
        return this.valid && this.open;
    }

    public double distance() {
        return this.distance;
    }

    public int distanceAsInt() {
        return (int) this.distance;
    }

    public double ensureDistanceLegal(double value) {
        int val = (int) value;
        if (val <= 0 || val > 100 || val % 20 != 0) {
            throw new IllegalArgumentException("Illegal Attack Distance:" + value);
        }
        return value;
    }

    public AttackMode mode() {
        return this.mode;
    }

    public ItemStack shadow() {
        if (!this.open()) {
            if (!this.shadow.isEmpty()) {
                this.shadow.setCount(0);
            }
            return ItemStack.EMPTY;
        }
        if (this.shadow.isEmpty()) {
            this.shadow = ZenithItems.ZENITH.get().instanceOf((int) this.distance, this.mode);
        }
        return this.shadow;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ZenithSlot that)) {
            return false;
        }
        return valid == that.valid
            && open == that.open
            && Double.compare(distance, that.distance) == 0
            && mode == that.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valid, open, distance, mode);
    }

    public static class Builder {
        private boolean valid = false;
        private boolean open = false;
        private double distance = 20;
        private AttackMode mode = AttackMode.LIVING_ENTITY;

        public static Builder of() {
            return new Builder();
        }

        public Builder valid(boolean value) {
            this.valid = value;
            return this;
        }

        public Builder open(boolean value) {
            this.open = value;
            return this;
        }

        public Builder distance(int value) {
            if (value <= 0 || value > 100 || value % 20 != 0) {
                throw new IllegalArgumentException("Invalid Attack Distance:" + value);
            }
            this.distance = value;
            return this;
        }

        public Builder mode(AttackMode mode) {
            this.mode = mode;
            return this;
        }

        public ZenithSlot build() {
            return new ZenithSlot(
                valid, open, distance, mode
            );
        }
    }
}
