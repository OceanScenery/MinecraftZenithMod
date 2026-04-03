package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.zenith_class.entity.ZenithProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZenithEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(
            ForgeRegistries.ENTITY_TYPES,
            TheZenithMod.MOD_ID
    );

    public static final RegistryObject<EntityType<ZenithProjectile>> ZENITH_PROJECTILE=ENTITIES.register(
            "zenith_projectile",
            ()->EntityType.Builder.<ZenithProjectile>of(
                            ZenithProjectile::new,
                            MobCategory.MISC
                    ).setUpdateInterval(1).clientTrackingRange(60).sized(1f,1f)
                    .fireImmune().build("zenith_projectile")
    );
}
