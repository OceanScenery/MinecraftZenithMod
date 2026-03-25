package com.oceanscenery.zenith.mod_class.data_component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

    public static final Codec<Distance> CODEC= RecordCodecBuilder.create(
            distanceInstance -> distanceInstance.group(
                    Codec.DOUBLE.fieldOf("distance").forGetter(Distance::dist)
            ).apply(distanceInstance,Distance::new)
    );
}
