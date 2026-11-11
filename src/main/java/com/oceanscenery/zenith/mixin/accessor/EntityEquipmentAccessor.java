package com.oceanscenery.zenith.mixin.accessor;

import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.EnumMap;

@Mixin(EntityEquipment.class)
public interface EntityEquipmentAccessor {
    @Accessor(value = "items")
    EnumMap<EquipmentSlot, ItemStack> zenith$items();
}
