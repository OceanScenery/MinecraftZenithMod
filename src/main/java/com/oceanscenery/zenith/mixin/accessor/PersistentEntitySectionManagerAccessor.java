package com.oceanscenery.zenith.mixin.accessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import java.util.UUID;

@Mixin(value = PersistentEntitySectionManager.class)
public interface PersistentEntitySectionManagerAccessor {
    @Accessor(value = "visibleEntityStorage")
    EntityLookup<Entity> zenith$getVisibleEntityStorage();

    @Accessor(value = "sectionStorage")
    EntitySectionStorage<?> zenith$getSectionStorage();

    @Accessor(value = "knownUuids")
    Set<UUID> zenith$getKnownUuids();
}
