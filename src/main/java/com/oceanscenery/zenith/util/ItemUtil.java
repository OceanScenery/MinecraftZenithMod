package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.registry.ZenithCapabilities;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.server.ServerLifecycleHooks;

public class ItemUtil {
    public static void checkExist(CompoundTag tag) {
        if (!tag.contains("Zenith")) {
            CompoundTag z_tag = new CompoundTag();
            z_tag.putLong("use_tick", 0);
            z_tag.putDouble("distance", 20);
            z_tag.putInt("attack_mode", 0);
            tag.put("Zenith", z_tag);
        }
    }

    public static int getAttackMode(ItemStack item) {
        if (!item.is(ZenithItems.ZENITH.get())) {
            return 0;
        }
        CompoundTag tag = item.getOrCreateTag();
        checkExist(tag);
        CompoundTag z_tag = tag.getCompound("Zenith");
        if (!z_tag.contains("attack_mode")) {
            z_tag.putInt("attack_mode", 0);
            return 0;
        }
        return z_tag.getInt("attack_mode");
    }

    public static long getLastUseTick(ItemStack item) {
        if (!item.is(ZenithItems.ZENITH.get())) {
            return 0;
        }
        CompoundTag tag = item.getOrCreateTag();
        checkExist(tag);
        CompoundTag z_tag = tag.getCompound("Zenith");
        if (!z_tag.contains("use_tick")) {
            z_tag.putLong("use_tick", 0);
            return 0;
        }
        return z_tag.getLong("use_tick");
    }

    public static double getDistance(ItemStack item) {
        if (!item.is(ZenithItems.ZENITH.get())) {
            return 20;
        }
        CompoundTag tag = item.getOrCreateTag();
        checkExist(tag);
        CompoundTag z_tag = tag.getCompound("Zenith");
        if (!z_tag.contains("distance")) {
            z_tag.putDouble("distance", 20);
            return 20;
        }
        return z_tag.getDouble("distance");
    }

    public static IZenithSlot getSlot(Player player) {
        return player.getCapability(
            ZenithCapabilities.ZENITH_SLOT
        ).resolve().orElse(null);
    }
}
