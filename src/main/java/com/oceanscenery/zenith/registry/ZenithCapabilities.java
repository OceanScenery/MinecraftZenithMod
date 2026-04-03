package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.capabilities.IZenithDamageMark;
import com.oceanscenery.zenith.registry.capabilities.ZenithDamageMark;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber
public class ZenithCapabilities {
    public static final Capability<IZenithDamageMark> ZENITH_MARK=CapabilityManager.get(
            new CapabilityToken<>() {
                @Override
                public String toString() {
                    return super.toString();
                }
            }
    );
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IZenithDamageMark.class);
    }
    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event){
        Entity entity=event.getObject();
        ZenithDamageMark zenithDamageMark = new ZenithDamageMark();
        LazyOptional<IZenithDamageMark> optional = LazyOptional.of(() -> zenithDamageMark);
        ICapabilitySerializable<CompoundTag> provider= new ICapabilitySerializable<CompoundTag>() {

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == ZENITH_MARK) {
                    return optional.cast();
                }
                return LazyOptional.empty();
            }

            @Override
            public CompoundTag serializeNBT() {
                return zenithDamageMark.serialize();
            }

            @Override
            public void deserializeNBT(CompoundTag nbt) {
                zenithDamageMark.deserialize(nbt);
            }
        };

        event.addCapability(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith_damage_mark"),provider);
        event.addListener(
                optional::invalidate
        );
    }
}
