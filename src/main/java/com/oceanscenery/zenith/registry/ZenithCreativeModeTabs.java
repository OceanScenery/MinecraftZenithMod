package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZenithCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS=DeferredRegister.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            TheZenithMod.MOD_ID
    );

    public static final RegistryObject<CreativeModeTab> ZENITH_TAB=CREATIVE_MODE_TABS.register(
            "zenith",
            ()-> CreativeModeTab.builder().icon(ZenithItems.ZENITH.get()::getDefaultInstance)
                    .title(Component.translatable("item.the_zenith_sword.zenith"))
                    .displayItems((parameters, output)->{
                        output.accept(ZenithItems.ZENITH.get());
                    }).build()
    );
}
