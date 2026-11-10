package com.oceanscenery.zenith.mixin.client;

import com.oceanscenery.zenith.mod_class.attachment.ZenithSlot;
import com.oceanscenery.zenith.util.AttachmentUtil;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmedEntityRenderState.class)
public class ArmedEntityRenderStateMixin {
    @Inject(method = "extractArmedEntityRenderState", at = @At("TAIL"))
    private static void zenith$extractArmedEntityRenderState(LivingEntity entity, ArmedEntityRenderState state
        , ItemModelResolver itemModelResolver, float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }
        ZenithSlot slot = AttachmentUtil.getSlot(player);
        if (!slot.open()) {
            return;
        }

        ItemStack shadow = slot.shadow();
        if (state.mainArm == HumanoidArm.RIGHT) {
            itemModelResolver.updateForLiving(
                state.rightHandItemState,
                shadow,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                entity
            );
            state.rightHandItemStack = shadow.copy();
        } else {
            itemModelResolver.updateForLiving(
                state.leftHandItemState,
                shadow,
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                entity
            );
            state.leftHandItemStack = shadow.copy();
        }
    }
}
