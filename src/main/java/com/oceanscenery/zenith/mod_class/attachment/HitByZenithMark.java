package com.oceanscenery.zenith.mod_class.attachment;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public record HitByZenithMark(boolean hit, long expireTime) {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HitByZenithMark(boolean hit1, long time))) {
            return false;
        }
        return hit == hit1 && expireTime == time;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hit, expireTime);
    }

    public boolean isActive(long gameTime) {
        return gameTime <= expireTime && this.hit;
    }

    public static final StreamCodec<ByteBuf, HitByZenithMark> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        HitByZenithMark::hit,
        ByteBufCodecs.VAR_LONG,
        HitByZenithMark::expireTime,
        HitByZenithMark::new
    );
}
