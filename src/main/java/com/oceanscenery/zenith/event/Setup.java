package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Deprecated
@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Setup {
    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
    public static void onSetup(FMLCommonSetupEvent event) {
    }
}
