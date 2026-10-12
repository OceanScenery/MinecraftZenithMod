package com.oceanscenery.zenith.mod_class.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

import java.util.Objects;

public record ZenithHealthMark(float value, long gameTime, boolean valid) {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ZenithHealthMark(float value1, long time, boolean valid1))) {
            return false;
        }
        return Float.compare(value, value1) == 0 && gameTime == time && valid == valid1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, gameTime, valid);
    }

    public static final Codec<ZenithHealthMark> CODEC = RecordCodecBuilder.create(
        zenithHealthMarkInstance -> zenithHealthMarkInstance.group(
            Codec.FLOAT.fieldOf("value").forGetter(ZenithHealthMark::value),
            Codec.LONG.fieldOf("gameTime").forGetter(ZenithHealthMark::gameTime),
            Codec.BOOL.fieldOf("valid").forGetter(ZenithHealthMark::valid)
        ).apply(zenithHealthMarkInstance, ZenithHealthMark::new)
    );

    public static final StreamCodec<ByteBuf, ZenithHealthMark> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT,
        ZenithHealthMark::value,
        ByteBufCodecs.LONG,
        ZenithHealthMark::gameTime,
        ByteBufCodecs.BOOL,
        ZenithHealthMark::valid,
        ZenithHealthMark::new
    );

    public boolean valid(Level level) {
        long c = level.getGameTime() - this.gameTime();
        if (c > 20 || c < 0) {
            return false;
        }
        return true;
    }
}
