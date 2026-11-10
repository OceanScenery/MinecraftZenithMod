package com.oceanscenery.zenith.mixin.accessor;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.level.entity.EntityLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(value = EntityLookup.class)
public interface EntityLookupAccessor {
    @Accessor(value = "byId")
    Int2ObjectMap<?> zenith$getById();

    @Accessor(value = "byUuid")
    Map<UUID, ?> zenith$byUuid();
}
