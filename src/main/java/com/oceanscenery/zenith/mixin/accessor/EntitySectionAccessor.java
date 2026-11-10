package com.oceanscenery.zenith.mixin.accessor;

import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.level.entity.EntitySection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = EntitySection.class)
public interface EntitySectionAccessor {
    @Accessor(value = "storage")
    ClassInstanceMultiMap<?> zenith$getStorage();
}
