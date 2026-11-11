package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.mixin.accessor.EntityEquipmentAccessor;
import com.oceanscenery.zenith.mixin.accessor.LivingEntityAccessor;
import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InHandItemUtil {
    public static boolean matchHoldZenith(Player player) {
        if (AttachmentUtil.hasSlotOpen(player)) {
            return true;
        }

        int selected = player.getInventory().getSelectedSlot();
        Inventory inv = player.getInventory();
        ItemStack m = Inventory.isHotbarSlot(selected) ? inv.getNonEquipmentItems().get(inv.getSelectedSlot()) : ItemStack.EMPTY;

        ItemStack o = ((EntityEquipmentAccessor) ((LivingEntityAccessor) player).zenith$equipment())
            .zenith$items().getOrDefault(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        if (m.is(ZenithItems.ZENITH.get()) || o.is(ZenithItems.ZENITH.get())) {
            return true;
        }
        return false;
    }
}
