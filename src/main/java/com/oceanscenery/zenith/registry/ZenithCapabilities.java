package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.zenith_class.capabilities.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public class ZenithCapabilities {
    public static final Capability<IZenithIdMark> ZENITH_ID_MARK=CapabilityManager.get(
            new CapabilityToken<>() {
                @Override
                public String toString() {
                    return super.toString();
                }
            }
    );
    public static final Capability<IZenithUuidMark> ZENITH_UUID_MARK=CapabilityManager.get(
            new CapabilityToken<>() {
                @Override
                public String toString() {
                    return super.toString();
                }
            }
    );
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IZenithIdMark.class);
        event.register(IZenithUuidMark.class);
    }
}
