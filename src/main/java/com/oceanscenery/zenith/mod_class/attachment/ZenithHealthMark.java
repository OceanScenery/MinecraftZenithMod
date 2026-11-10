package com.oceanscenery.zenith.mod_class.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.oceanscenery.zenith.util.AttachmentUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;

import java.util.Objects;

public record ZenithHealthMark(float value, boolean valid) {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ZenithHealthMark(float value1, boolean valid1))) {
            return false;
        }
        return Float.compare(value, value1) == 0 && valid == valid1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, valid);
    }

    public static final Codec<ZenithHealthMark> CODEC = RecordCodecBuilder.create(
        zenithHealthMarkInstance -> zenithHealthMarkInstance.group(
            Codec.FLOAT.fieldOf("value").forGetter(ZenithHealthMark::value),
            Codec.BOOL.fieldOf("valid").forGetter(ZenithHealthMark::valid)
        ).apply(zenithHealthMarkInstance, ZenithHealthMark::new)
    );

    public static final StreamCodec<ByteBuf, ZenithHealthMark> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT,
        ZenithHealthMark::value,
        ByteBufCodecs.BOOL,
        ZenithHealthMark::valid,
        ZenithHealthMark::new
    );

    public boolean valid(Entity entity) {
        return this.valid && AttachmentUtil.hitByZenith(entity);
    }
}
