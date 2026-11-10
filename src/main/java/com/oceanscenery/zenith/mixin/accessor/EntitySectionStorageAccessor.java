package com.oceanscenery.zenith.mixin.accessor;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = EntitySectionStorage.class)
public interface EntitySectionStorageAccessor {
    @Accessor(value = "sections")
    Long2ObjectMap<EntitySection<?>> zenith$getSections();

    @Accessor(value = "sectionIds")
    LongSortedSet zenith$getSectionIds();
}
