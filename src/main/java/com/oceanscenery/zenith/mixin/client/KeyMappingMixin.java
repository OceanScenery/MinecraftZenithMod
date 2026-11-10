package com.oceanscenery.zenith.mixin.client;

import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithSlot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KeyMapping.class, priority = 114514)
public abstract class KeyMappingMixin {
    @Accessor(value = "clickCount")
    abstract void setClickCount(int value);

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    public void onIsDown(CallbackInfoReturnable<Boolean> cir) {
        if (!ZenithConfigs.CLIENT_CONFIG.isLoaded() || !ZenithConfigs.ZENITH_CLIENT_CONFIG.BLOCK_INTERACTION.get()) {
            return;
        }
        KeyMapping self = (KeyMapping) (Object) this;
        if (self == Minecraft.getInstance().options.keyUse) {
            if (Minecraft.getInstance().cameraEntity instanceof Player player) {
                IZenithSlot slot;
                if (player.getMainHandItem().is(ZenithItems.ZENITH.get())
                    || player.getOffhandItem().is(ZenithItems.ZENITH.get())
                    || ((slot = CapabilitiesHandler.getSlot(player)) != null && slot.open())) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    public void onConsumeClick(CallbackInfoReturnable<Boolean> cir) {
        if (!ZenithConfigs.CLIENT_CONFIG.isLoaded() || !ZenithConfigs.ZENITH_CLIENT_CONFIG.BLOCK_INTERACTION.get()) {
            return;
        }
        KeyMapping self = (KeyMapping) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        if (self == minecraft.options.keyUse && minecraft.player != null) {
            IZenithSlot slot;
            if (minecraft.player.getMainHandItem().is(ZenithItems.ZENITH.get())
                || minecraft.player.getOffhandItem().is(ZenithItems.ZENITH.get())
                || ((slot = CapabilitiesHandler.getSlot(minecraft.player)) != null && slot.open())) {
                this.setClickCount(0);
                cir.setReturnValue(false);
            }
        }
    }
}
