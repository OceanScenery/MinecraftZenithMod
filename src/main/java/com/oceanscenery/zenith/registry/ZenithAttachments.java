package com.oceanscenery.zenith.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.oceanscenery.zenith.TheZenithMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ZenithAttachments {
    public static Codec<UUID> UUID_CODEC=Codec.STRING.flatXmap(
            str->{
                try{
                    return DataResult.success(UUID.fromString(str));
                }catch(Exception e){
                    return DataResult.error(()->"Invalid UUID:"+str);
                }
            },
            uuid->DataResult.success(uuid.toString())
    );

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS=DeferredRegister.create(
            NeoForgeRegistries.ATTACHMENT_TYPES,
            TheZenithMod.MOD_ID
    );

    public static final DeferredHolder<AttachmentType<?>,AttachmentType<Set<UUID>>> ZENITH_PLAYER_MARK=ATTACHMENTS.register(
            "zenith_player_mark",
            resourceLocation -> AttachmentType.<Set<UUID>>builder(
                    iAttachmentHolder -> HashSet.newHashSet(4)
            ).serialize(
                    MapCodec.assumeMapUnsafe(
                            UUID_CODEC.listOf().xmap(
                                    HashSet::new,
                                    set->set.stream().toList()
                            )
                    ),
                    set->!set.isEmpty()
            ).copyOnDeath().build()
    );

    public static final DeferredHolder<AttachmentType<?>,AttachmentType<Set<String>>> ZENITH_ID_MARK=ATTACHMENTS.register(
            "zenith_id_mark",
            resourceLocation -> AttachmentType.<Set<String>>builder(
                    iAttachmentHolder -> HashSet.newHashSet(4)
            ).serialize(
                    MapCodec.assumeMapUnsafe(
                            Codec.STRING.listOf().xmap(
                                    HashSet::new,
                                    set->set.stream().toList()
                            )
                    ),
                    set->!set.isEmpty()
            ).copyOnDeath().build()
    );
}
