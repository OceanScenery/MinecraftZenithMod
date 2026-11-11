package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InHandItemUtil {
    public static boolean matchHoldZenith(Player player) {
        if (AttachmentUtil.hasSlotOpen(player)) {
            return true;
        }

        int selected = player.getInventory().selected;
        Inventory inv = player.getInventory();
        ItemStack m = Inventory.isHotbarSlot(selected) ? inv.items.get(inv.selected) : ItemStack.EMPTY;

        ItemStack o = inv.offhand.getFirst();
        if (m.is(ZenithItems.ZENITH.get()) || o.is(ZenithItems.ZENITH.get())) {
            return true;
        }
        return false;
    }
}
