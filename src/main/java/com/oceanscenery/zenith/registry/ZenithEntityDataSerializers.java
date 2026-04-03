package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.tool.PosUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public class ZenithEntityDataSerializers {
    public static final DeferredRegister<EntityDataSerializer<?>> ENTITY_DATA_SERIALIZERS=DeferredRegister.create(
            ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS,
            TheZenithMod.MOD_ID
    );

    public static final RegistryObject<EntityDataSerializer<Double>> DOUBLE=ENTITY_DATA_SERIALIZERS.register(
            "double",
            () -> new EntityDataSerializer<>() {
                @Override
                public void write(@NotNull FriendlyByteBuf buf, @NotNull Double value) {
                    buf.writeDouble(value);
                }

                @Override
                public @NotNull Double read(@NotNull FriendlyByteBuf buf) {
                    return buf.readDouble();
                }

                @Override
                public @NotNull Double copy(@NotNull Double value) {
                    return value;
                }
            }
    );

    public static final RegistryObject<EntityDataSerializer<PosUtil.Rotation>> ROTATION=ENTITY_DATA_SERIALIZERS.register(
            "rotation",
            ()->new EntityDataSerializer<PosUtil.Rotation>() {
                @Override
                public void write(@NotNull FriendlyByteBuf buf, PosUtil.@NotNull Rotation rot) {
                    buf.writeDouble(rot.getPitch());
                    buf.writeDouble(rot.getYaw());
                }

                @Override
                public PosUtil.@NotNull Rotation read(@NotNull FriendlyByteBuf buf) {
                    return new PosUtil.Rotation(buf.readDouble(), buf.readDouble());
                }

                @Override
                public PosUtil.@NotNull Rotation copy(PosUtil.@NotNull Rotation rot) {
                    return new PosUtil.Rotation(rot.getPitch(),rot.getYaw());
                }
            }
    );
}
