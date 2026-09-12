package com.oceanscenery.zenith.mod_class.data_component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record LastUseTime(long tickTime) {
    @Override
    public long tickTime() {
        return tickTime;
    }

    @Override
    public @NotNull String toString() {
        return "tick:"+tickTime;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LastUseTime(long time))) return false;
        return tickTime == time;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tickTime);
    }

    public static final Codec<LastUseTime> CODEC=Codec.LONG.xmap(
            LastUseTime::new,
            LastUseTime::tickTime
    );

    public static final StreamCodec<ByteBuf,LastUseTime> STREAM_CODEC=ByteBufCodecs.VAR_LONG.map(
            LastUseTime::new,
            LastUseTime::tickTime
    );
}
