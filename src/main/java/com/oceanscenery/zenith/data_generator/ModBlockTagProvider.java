package com.oceanscenery.zenith.data_generator;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {


    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, TheZenithMod.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

    }
}
