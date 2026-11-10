package com.oceanscenery.zenith.server.packet;

import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.util.ItemUtil;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ZenithPacket(int id, int operation, int slot) implements IZenithPacket {
    public ZenithPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeInt(operation);
        buf.writeInt(slot);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player.getId() != this.id()) {
            context.get().setPacketHandled(true);
            return;
        }
        if (slot >= player.getInventory().getContainerSize() || slot < 0) {
            context.get().setPacketHandled(true);
            return;
        }
        if (operation == 0) {
            IZenithSlot shadow = CapabilitiesHandler.getSlot(player);
            if (shadow != null && shadow.open()) {
                ZenithItems.ZENITH.get().attack(
                    shadow.get(), player, player.level(), InteractionHand.MAIN_HAND
                );
                context.get().setPacketHandled(true);
                return;
            }
            if (player.getMainHandItem().is(ZenithItems.ZENITH.get())) {
                ZenithItems.ZENITH.get().attack(player.getMainHandItem(), player, player.level(), InteractionHand.MAIN_HAND);
                context.get().setPacketHandled(true);
                return;
            }
            if (player.getOffhandItem().is(ZenithItems.ZENITH.get())) {
                ZenithItems.ZENITH.get().attack(player.getOffhandItem(), player, player.level(), InteractionHand.OFF_HAND);
                context.get().setPacketHandled(true);
                return;
            }
        }
        if (operation == 1) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ZenithItems.ZENITH.get())) {
                double dist = ItemUtil.getDistance(stack);
                if (stack.getTag() != null) {
                    stack.getTag().getCompound("Zenith").putDouble("distance", (int) dist % 100 + 20);
                }
            }
            context.get().setPacketHandled(true);
            return;
        }
        if (operation == 2) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ZenithItems.ZENITH.get())) {
                int mode = ItemUtil.getAttackMode(stack);
                if (stack.getTag() != null) {
                    stack.getTag().getCompound("Zenith").putInt("attack_mode", (mode + 1) % 3);
                }
            }
            context.get().setPacketHandled(true);
            return;
        }
    }
}
