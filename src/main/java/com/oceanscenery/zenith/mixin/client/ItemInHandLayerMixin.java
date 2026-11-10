package com.oceanscenery.zenith.mixin.client;

import com.oceanscenery.zenith.util.AttachmentUtil;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {

    @Redirect(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack zenith$getMainHandItem(LivingEntity entity) {
        ItemStack original = entity.getMainHandItem();
        if (!(entity instanceof Player player)) {
            return original;
        }

        return AttachmentUtil.getSlot(player).open() ? AttachmentUtil.getSlot(player).shadow() : original;
    }
}
