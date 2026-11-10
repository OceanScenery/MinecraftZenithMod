package com.oceanscenery.zenith.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.oceanscenery.zenith.util.AttachmentUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @ModifyVariable(
        method = "renderArmWithItem",
        at = @At("HEAD"),
        argsOnly = true,
        index = 6
    )
    private ItemStack zenith$renderArmWithItem(ItemStack origin, @Local(argsOnly = true, ordinal = 0) AbstractClientPlayer player
        , @Local(argsOnly = true, ordinal = 0) InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return origin;
        }
        if(AttachmentUtil.getSlot(player).open()){
            return AttachmentUtil.getSlot(player).shadow();
        }
        return origin;
    }
}
