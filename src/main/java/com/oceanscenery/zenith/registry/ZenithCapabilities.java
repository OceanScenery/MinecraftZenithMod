package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.zenith_class.capabilities.*;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZenithCapabilities {
    public static final Capability<IZenithIdMark> ZENITH_ID_MARK = CapabilityManager.get(
        new CapabilityToken<>() {
            @Override
            public String toString() {
                return super.toString();
            }
        }
    );
    public static final Capability<IZenithUuidMark> ZENITH_UUID_MARK = CapabilityManager.get(
        new CapabilityToken<>() {
            @Override
            public String toString() {
                return super.toString();
            }
        }
    );
    public static final Capability<IHitByZenithMark> HIT_BY_ZENITH_MARK = CapabilityManager.get(
        new CapabilityToken<>() {
            @Override
            public String toString() {
                return super.toString();
            }
        }
    );

    public static final Capability<IZenithHealthMark> ZENITH_HEALTH_MARK = CapabilityManager.get(
        new CapabilityToken<>() {
            @Override
            public String toString() {
                return super.toString();
            }
        }
    );

    public static final Capability<IZenithDamageMark> ZENITH_DAMAGE_MARK = CapabilityManager.get(
        new CapabilityToken<IZenithDamageMark>() {
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
        event.register(IHitByZenithMark.class);
        event.register(IZenithHealthMark.class);
        event.register(IZenithDamageMark.class);
    }
}
