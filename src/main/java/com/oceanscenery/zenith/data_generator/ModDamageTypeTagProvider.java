package com.oceanscenery.zenith.data_generator;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithDamageType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModDamageTypeTagProvider extends DamageTypeTagsProvider {

    public ModDamageTypeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, TheZenithMod.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(DamageTypeTags.NO_KNOCKBACK).add(ZenithDamageType.ZENITH);
        tag(DamageTypeTags.IS_PLAYER_ATTACK).add(ZenithDamageType.ZENITH).add(ZenithDamageType.ZENITH_KNOCKBACK);
        tag(DamageTypeTags.BYPASSES_COOLDOWN).add(ZenithDamageType.ZENITH).add(ZenithDamageType.ZENITH_KNOCKBACK);
        tag(DamageTypeTags.BYPASSES_SHIELD).add(ZenithDamageType.ZENITH).add(ZenithDamageType.ZENITH_KNOCKBACK);
    }
}
