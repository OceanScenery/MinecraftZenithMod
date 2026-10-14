package com.oceanscenery.zenith.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ClientLevel.class)
public interface ClientLevelAccessor {
    @Accessor(value = "entityStorage")
    public TransientEntitySectionManager<Entity> getEntityStorage();
}
