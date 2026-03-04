package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.entity.ZenithProjectile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntity {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(
            BuiltInRegistries.ENTITY_TYPE,
            TheZenithMod.MOD_ID
    );

    public static final DeferredHolder<EntityType<?>,EntityType<ZenithProjectile>> ZENITH_PROJECTILE=ENTITIES.register(
            "zenith_projectile",
            ()->EntityType.Builder.<ZenithProjectile>of(
                    ZenithProjectile::new,
                    MobCategory.MISC
            ).setUpdateInterval(1).clientTrackingRange(60).sized(1f,1f)
                    .fireImmune().build("zenith_projectile")
    );
}
