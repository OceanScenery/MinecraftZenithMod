package com.oceanscenery.zenith.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.oceanscenery.zenith.mod_class.attachment.ZenithSlot;
import com.oceanscenery.zenith.util.AttachmentUtil;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FirstPersonHandsAndItems.class)
public class FirstPersonHandsAndItemsMixin {
    @ModifyExpressionValue(
        method = "extractRenderState",
        at = @At(value = "FIELD",
        target = "Lnet/minecraft/client/player/FirstPersonHandsAndItems;mainHandItem:Lnet/minecraft/world/item/ItemStack;",
            opcode = Opcodes.GETFIELD)
    )
    private ItemStack zenith$extractMainHandItem(ItemStack original, @Local(argsOnly = true, name = "player") LocalPlayer player) {
        ZenithSlot slot = AttachmentUtil.getSlot(player);
        return slot.open() ? slot.shadow() : original;
    }
}
