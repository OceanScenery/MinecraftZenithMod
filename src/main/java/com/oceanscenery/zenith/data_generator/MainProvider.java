package com.oceanscenery.zenith.data_generator;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber
public class MainProvider {
    @SubscribeEvent
    public static void generate(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper fileHelper=event.getExistingFileHelper();

        generator.addProvider(
                true,
                new ModItemModelProvider(
                        packOutput,
                        fileHelper
                )
        );

        generator.addProvider(
                true,
                new ModLanguageProvider(
                        packOutput,"zh_cn"
                )
        );

        generator.addProvider(
                true,
                new ModLanguageProvider(
                        packOutput,"en_us"
                )
        );

        ModBlockTagProvider modBlockTagProvider=new ModBlockTagProvider(
                packOutput,lookupProvider,fileHelper
        );

        generator.addProvider(
                true,
                modBlockTagProvider
        );

        generator.addProvider(
                true,
                new ModItemTagProvider(
                        packOutput,lookupProvider,modBlockTagProvider.contentsGetter(),fileHelper
                )
        );

        generator.addProvider(true
        ,new ModRecipeProvider(packOutput,lookupProvider));
    }
}
