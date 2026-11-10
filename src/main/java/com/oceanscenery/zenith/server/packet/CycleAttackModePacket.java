package com.oceanscenery.zenith.server.packet;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.registry.ZenithDataComponents;
import com.oceanscenery.zenith.registry.ZenithItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public record CycleAttackModePacket(int slot) implements CustomPacketPayload {
    public static final Type<CycleAttackModePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID, "zenith_cycle_attack_mode"));
    public static final Identifier ID = Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID, "zenith_cycle_attack_mode");

    public static final StreamCodec<ByteBuf, CycleAttackModePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, CycleAttackModePacket::slot,
        CycleAttackModePacket::new
    );

    public static void handle(final CycleAttackModePacket packet, final IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            if (packet.slot < 0 || packet.slot >= player.getInventory().getContainerSize()) {
                return;
            }

            ItemStack item = player.getInventory().getItem(packet.slot);
            if (item.is(ZenithItems.ZENITH.get())) {
                if (item.get(ZenithDataComponents.ATTACK_MODE) == null) {
                    item.set(ZenithDataComponents.ATTACK_MODE.get(), AttackMode.LIVING_ENTITY);
                } else {
                    item.set(
                        ZenithDataComponents.ATTACK_MODE.get(),
                        Optional.ofNullable(item.get(ZenithDataComponents.ATTACK_MODE)).map(
                            AttackMode::next
                        ).orElse(AttackMode.LIVING_ENTITY)
                    );
                }

                player.sendSystemMessage(
                    Component.translatable("the_zenith_sword.packet.attack_mode").append(
                        ":" + Optional.ofNullable(item.get(ZenithDataComponents.ATTACK_MODE))
                            .map(AttackMode::entityMode).orElse("living_entity")
                    ),
                    true
                );
            }
        }
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public Identifier getId() {
        return ID;
    }
}
