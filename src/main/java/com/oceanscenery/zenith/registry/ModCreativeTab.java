package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB=DeferredRegister.create(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            TheZenithMod.MOD_ID
    );

    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> ZENITH_TAB=CREATIVE_TAB.register(
            "zenith",
            registryName-> CreativeModeTab.builder().icon(ModItems.ZENITH::toStack)
                    .title(Component.translatable("item.the_zenith_sword.zenith"))
                    .displayItems((parameters, output)->{
                        output.accept(ModItems.ZENITH.get());
                    }).build()
    );
}
