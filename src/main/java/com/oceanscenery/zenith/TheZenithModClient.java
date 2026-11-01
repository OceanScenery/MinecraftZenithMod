package com.oceanscenery.zenith;

import com.oceanscenery.zenith.client.ZenithConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@OnlyIn(Dist.CLIENT)
public final class TheZenithModClient {
    private TheZenithModClient() {
    }

    public static void register(FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (minecraft, parent) -> new ZenithConfigScreen(parent)
            )
        );
    }
}
