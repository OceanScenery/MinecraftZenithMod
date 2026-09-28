package com.oceanscenery.zenith.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor(value = "DATA_HEALTH_ID")
    static EntityDataAccessor<Float> getHealthId() {
        return null;
    }

    @Invoker(value = "dropAllDeathLoot")
    void callDropAllDeathLoot(ServerLevel p_level, DamageSource damageSource);

    @Invoker(value = "checkTotemDeathProtection")
    boolean callCheckTotemDeathProtection(DamageSource source);
}
