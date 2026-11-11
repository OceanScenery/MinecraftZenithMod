package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InHandItemUtil {
    public static boolean matchHoldZenith(Player player) {
        IZenithSlot slot = CapabilitiesHandler.getSlot(player);
        if (slot != null && slot.open()) {
            return true;
        }

        int selected = player.getInventory().selected;
        Inventory inv = player.getInventory();
        ItemStack m = Inventory.isHotbarSlot(selected) ? inv.items.get(inv.selected) : ItemStack.EMPTY;

        ItemStack o = inv.offhand.get(0);
        if (m.is(ZenithItems.ZENITH.get()) || o.is(ZenithItems.ZENITH.get())) {
            return true;
        }
        return false;
    }
}
