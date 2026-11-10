package com.oceanscenery.zenith.mod_class.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public record ZenithHitMark(boolean hit, long generateTime, long expireTime) {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ZenithHitMark(boolean hit1, long time, long expireTime1))) {
            return false;
        }
        return hit == hit1 && expireTime == expireTime1 && generateTime == time;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hit, generateTime, expireTime);
    }

    public boolean isActive(long gameTime) {
        return gameTime <= expireTime && gameTime >= generateTime && this.hit;
    }

    public static final MapCodec<ZenithHitMark> MAP_CODEC = RecordCodecBuilder.mapCodec(
        ins -> ins.group(
            Codec.BOOL.fieldOf("hit").forGetter(ZenithHitMark::hit),
            Codec.LONG.fieldOf("generate_time").forGetter(ZenithHitMark::generateTime),
            Codec.LONG.fieldOf("expire_time").forGetter(ZenithHitMark::expireTime)
        ).apply(ins, ZenithHitMark::new)
    );

    public static final Codec<ZenithHitMark> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<ByteBuf, ZenithHitMark> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        ZenithHitMark::hit,
        ByteBufCodecs.VAR_LONG,
        ZenithHitMark::generateTime,
        ByteBufCodecs.VAR_LONG,
        ZenithHitMark::expireTime,
        ZenithHitMark::new
    );
}
