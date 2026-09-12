package com.oceanscenery.zenith.mod_class.data_component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Distance(double dist) {

    @Override
    public @NotNull String toString() {
        return "Distance{" +
                "dist=" + dist +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Distance(double dist1))) return false;
        return Double.compare(dist, dist1) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dist);
    }

    public static final Codec<Distance> CODEC=Codec.DOUBLE.xmap(
            Distance::new,
            Distance::dist
    );

    public static final StreamCodec<ByteBuf,Distance> STREAM_CODEC=ByteBufCodecs.DOUBLE.map(
            Distance::new,
            Distance::dist
    );
}
