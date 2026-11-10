package com.oceanscenery.zenith.mixin.accessor;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = EntityTickList.class)
public interface EntityTickListAccessor {
    @Invoker(value = "ensureActiveIsNotIterated")
    void zenith$ensureActiveIsNotIterated();

    @Accessor(value = "passive")
    Int2ObjectMap<Entity> zenith$getPassive();

    @Accessor(value = "active")
    Int2ObjectMap<Entity> zenith$getActive();

    @Accessor(value = "iterated")
    Int2ObjectMap<Entity> zenith$getIterated();
}
