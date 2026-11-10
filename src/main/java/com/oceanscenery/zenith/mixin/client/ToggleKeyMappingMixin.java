package com.oceanscenery.zenith.mixin.client;

import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.util.AttachmentUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ToggleKeyMapping.class)
public class ToggleKeyMappingMixin {
    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    public void onIsDown(CallbackInfoReturnable<Boolean> cir) {
        if (!ZenithConfigs.CLIENT_CONFIG.isLoaded() || !ZenithConfigs.ZENITH_CLIENT_CONFIG.BLOCK_INTERACTION.get()) {
            return;
        }
        KeyMapping self = (KeyMapping) (Object) this;
        if (self == Minecraft.getInstance().options.keyUse) {
            if (Minecraft.getInstance().getCameraEntity() instanceof Player player) {
                if (player.getMainHandItem().is(ZenithItems.ZENITH.get())
                    || player.getOffhandItem().is(ZenithItems.ZENITH.get())
                    || AttachmentUtil.getSlot(player).open()) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
