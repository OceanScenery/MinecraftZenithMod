package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.tool.PosUtil;
import com.oceanscenery.zenith.tool.Vector3;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ZenithEntityDataSerializer {
    public static final DeferredRegister<EntityDataSerializer<?>> MOD_ENTITY_DATA_SERIALIZER=DeferredRegister.create(
            NeoForgeRegistries.ENTITY_DATA_SERIALIZERS,
            TheZenithMod.MOD_ID
    );

    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<Vector3>> VECTOR=MOD_ENTITY_DATA_SERIALIZER.register(
            "vector",
            ()->EntityDataSerializer.forValueType(
                Vector3.STREAM_CODEC
            )
    );

    public static final DeferredHolder<EntityDataSerializer<?>,EntityDataSerializer<Double>> DOUBLE=MOD_ENTITY_DATA_SERIALIZER.register(
            "double",
            ()->EntityDataSerializer.forValueType(
                    ByteBufCodecs.DOUBLE
            )
    );

    public static final DeferredHolder<EntityDataSerializer<?>,EntityDataSerializer<PosUtil.Rotation>> ROTATION=MOD_ENTITY_DATA_SERIALIZER.register(
            "rotation",
            ()->EntityDataSerializer.forValueType(
                    PosUtil.STREAM_CODEC
            )
    );
}
