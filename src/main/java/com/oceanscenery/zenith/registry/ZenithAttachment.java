package com.oceanscenery.zenith.registry;

import com.mojang.serialization.Codec;
import com.oceanscenery.zenith.TheZenithMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ZenithAttachment {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS=DeferredRegister.create(
            NeoForgeRegistries.ATTACHMENT_TYPES,
            TheZenithMod.MOD_ID
    );

    public static final DeferredHolder<AttachmentType<?>,AttachmentType<Float>> ZENITH_INITIAL_DAMAGE_MARK=ATTACHMENTS.register(
            "zenith_initial_damage_mark",
            resourceLocation -> AttachmentType.<Float>builder(
                    iAttachmentHolder -> 0f
            ).serialize(Codec.FLOAT,aFloat -> true).build()
    );
}
