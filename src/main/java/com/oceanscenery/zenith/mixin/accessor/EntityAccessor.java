package com.oceanscenery.zenith.mixin.accessor;

import com.ibm.icu.impl.coll.CollationKeys;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.LevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Entity.class, priority = 1500)
public interface EntityAccessor {
    @Accessor(value = "entityData")
    SynchedEntityData getRealEntityData();

    @Accessor(value = "removalReason")
    Entity.RemovalReason zenith$getRemovalReason();

    @Accessor(value = "removalReason")
    void zenith$setRemovalReason(Entity.RemovalReason reason);

    @Accessor(value = "levelCallback")
    EntityInLevelCallback zenith$getLevelCallback();

    @Accessor(value = "levelCallback")
    void zenith$setLevelCallback(EntityInLevelCallback callback);
}
