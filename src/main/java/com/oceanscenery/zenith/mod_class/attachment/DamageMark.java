package com.oceanscenery.zenith.mod_class.attachment;

import com.mojang.serialization.Codec;
import com.oceanscenery.zenith.mod_class.config.InvalidDamageAction;
import com.oceanscenery.zenith.util.ConfigUtil;
import net.minecraft.core.UUIDUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DamageMark {
    private final HashMap<UUID, Float> marks;

    public static final Codec<DamageMark> CODEC = Codec.unboundedMap(
        UUIDUtil.STRING_CODEC,
        Codec.FLOAT
    ).xmap(
        DamageMark::new,
        DamageMark::getMap
    );

    public DamageMark() {
        this.marks = new HashMap<>();
    }

    public DamageMark(Map<UUID, Float> map) {
        this.marks = new HashMap<>(map);
    }

    public HashMap<UUID, Float> getMap() {
        if (ConfigUtil.invalidAction() != InvalidDamageAction.DELAYED) {
            this.marks.clear();
        }
        return this.marks;
    }

    public boolean isEmpty() {
        return this.marks.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DamageMark that)) {
            return false;
        }
        return Objects.equals(marks, that.marks);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(marks);
    }
}
