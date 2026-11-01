package com.oceanscenery.zenith.data_gen;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGen {
    @SubscribeEvent
    public static void Generate(GatherDataEvent event) {

        DataGenerator generator = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        var blockTag = new ModBlockTagProvider(packOutput, lookupProvider, existingFileHelper);
        generator.addProvider(
            true,
            blockTag
        );

        generator.addProvider(
            true,
            new ModItemTagProvider(packOutput, lookupProvider, blockTag.contentsGetter(), existingFileHelper)
        );

        generator.addProvider(
            true,
            new ModDamageTypeTagProvider(packOutput, lookupProvider, existingFileHelper)
        );

        generator.addProvider(
            true,
            new ModItemModelProvider(packOutput, existingFileHelper)
        );

        generator.addProvider(
            true,
            new ModLanguageProvider(packOutput, "zh_cn")
        );
        generator.addProvider(
            true,
            new ModLanguageProvider(packOutput, "en_us")
        );

        generator.addProvider(
            true,
            new ModRecipeProvider(packOutput, lookupProvider)
        );
    }
}
