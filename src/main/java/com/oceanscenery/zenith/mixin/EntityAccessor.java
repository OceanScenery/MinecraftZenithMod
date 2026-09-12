package com.oceanscenery.zenith.mixin;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Entity.class,priority = 1500)
public interface EntityAccessor {
    @Accessor(value = "entityData")
    SynchedEntityData getRealEntityData();
}
