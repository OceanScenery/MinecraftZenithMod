package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.attachment.*;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashSet;

public class ZenithAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(
        NeoForgeRegistries.ATTACHMENT_TYPES,
        TheZenithMod.MOD_ID
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ZenithIdMark>> ZENITH_ID_MARK = ATTACHMENTS.register(
        "zenith_id_mark",
        resourceLocation -> AttachmentType.builder(
            iAttachmentHolder -> new ZenithIdMark(new HashSet<>())
        ).serialize(
            ZenithIdMark.CODEC,
            zenithIdMark -> !zenithIdMark.ids().isEmpty()
        ).copyOnDeath().build()
    );
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ZenithUuidMark>> ZENITH_PLAYER_MARK = ATTACHMENTS.register(
        "zenith_player_mark",
        resourceLocation -> AttachmentType.builder(
            iAttachmentHolder -> new ZenithUuidMark(new HashSet<>())
        ).serialize(
            ZenithUuidMark.CODEC,
            zenithUuidMark -> !zenithUuidMark.uuids().isEmpty()
        ).copyOnDeath().build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ZenithDamageMark>> DAMAGE_MARK = ATTACHMENTS.register(
        "zenith_damage_mark",
        identifier -> AttachmentType.<ZenithDamageMark>builder(
            iAttachmentHolder -> new ZenithDamageMark()
        ).serialize(
            ZenithDamageMark.CODEC
        ).build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ZenithHitMark>> HIT_BY_ZENITH_MARK = ATTACHMENTS.register(
        "hit_by_zenith_mark",
        identifier -> AttachmentType.<ZenithHitMark>builder(
                iAttachmentHolder -> new ZenithHitMark(false, 0, 0)
            ).serialize(ZenithHitMark.CODEC)
            .sync(ZenithHitMark.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ZenithHealthMark>> ZENITH_HEALTH_MARK = ATTACHMENTS.register(
        "zenith_health",
        identifier -> AttachmentType.<ZenithHealthMark>builder(
                iAttachmentHolder -> new ZenithHealthMark(0f, false)
            ).serialize(ZenithHealthMark.CODEC)
            .sync(ZenithHealthMark.STREAM_CODEC)
            .build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ZenithSlot>> ZENITH_SLOT = ATTACHMENTS.register(
        "zenith_slot",
        iden -> AttachmentType.builder(
                iAttachmentHolder -> ZenithSlot.Builder.of().build()
            ).serialize(ZenithSlot.CODEC)
            .sync(ZenithSlot.STREAM_CODEC)
            .copyOnDeath()
            .build()
    );
}
