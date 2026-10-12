package com.oceanscenery.zenith.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.attachment.DamageMark;
import com.oceanscenery.zenith.mod_class.attachment.HitByZenithMark;
import com.oceanscenery.zenith.mod_class.attachment.ZenithHealthMark;
import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ZenithAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(
        NeoForgeRegistries.ATTACHMENT_TYPES,
        TheZenithMod.MOD_ID
    );
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Set<String>>> ZENITH_ID_MARK = ATTACHMENTS.register(
        "zenith_id_mark",
        resourceLocation -> AttachmentType.<Set<String>>builder(
            iAttachmentHolder -> HashSet.newHashSet(4)
        ).serialize(
            MapCodec.assumeMapUnsafe(
                Codec.STRING.listOf().xmap(
                    HashSet::new,
                    set -> set.stream().toList()
                )
            ),
            set -> !set.isEmpty()
        ).copyOnDeath().build()
    );
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Set<UUID>>> ZENITH_PLAYER_MARK = ATTACHMENTS.register(
        "zenith_player_mark",
        resourceLocation -> AttachmentType.<Set<UUID>>builder(
            iAttachmentHolder -> HashSet.newHashSet(4)
        ).serialize(
            MapCodec.assumeMapUnsafe(
                UUIDUtil.STRING_CODEC.listOf().xmap(
                    HashSet::new,
                    set -> set.stream().toList()
                )
            ),
            set -> !set.isEmpty()
        ).copyOnDeath().build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DamageMark>> DAMAGE_MARK = ATTACHMENTS.register(
        "zenith_damage_mark",
        identifier -> AttachmentType.<DamageMark>builder(
            iAttachmentHolder -> new DamageMark()
        ).serialize(
            MapCodec.assumeMapUnsafe(DamageMark.CODEC)
        ).build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<HitByZenithMark>> HIT_BY_ZENITH_MARK = ATTACHMENTS.register(
        "hit_by_zenith_mark",
        identifier -> AttachmentType.<HitByZenithMark>builder(
            iAttachmentHolder -> new HitByZenithMark(false, 0)
        ).sync(HitByZenithMark.STREAM_CODEC).build()
    );

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ZenithHealthMark>> ZENITH_HEALTH_MARK = ATTACHMENTS.register(
        "zenith_health",
        identifier -> AttachmentType.<ZenithHealthMark>builder(
                iAttachmentHolder -> new ZenithHealthMark(0f, 0,false)
            ).serialize(MapCodec.assumeMapUnsafe(ZenithHealthMark.CODEC))
            .sync(ZenithHealthMark.STREAM_CODEC)
            .build()
    );
}
