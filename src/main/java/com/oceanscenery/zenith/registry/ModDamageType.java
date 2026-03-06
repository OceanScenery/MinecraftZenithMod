package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageType {
    public static final ResourceKey<DamageType> ZENITH=ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith")
    );
}
