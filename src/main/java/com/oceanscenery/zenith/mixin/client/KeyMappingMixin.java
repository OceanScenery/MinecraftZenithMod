package com.oceanscenery.zenith.mixin.client;

import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.util.InHandItemUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KeyMapping.class, priority = 114514)
public abstract class KeyMappingMixin {
    @Unique
    private boolean zenith$isDown = false;
    @Unique
    private boolean zenith$consumeClick = false;

    @Accessor(value = "clickCount")
    abstract void setClickCount(int value);

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    public void onIsDown(CallbackInfoReturnable<Boolean> cir) {
        if (zenith$isDown) {
            return;
        }
        try {
            zenith$isDown = true;
            if (!ZenithConfigs.CLIENT_CONFIG.isLoaded() || !ZenithConfigs.ZENITH_CLIENT_CONFIG.BLOCK_INTERACTION.get()) {
                return;
            }
            KeyMapping self = (KeyMapping) (Object) this;
            if (self == Minecraft.getInstance().options.keyUse) {
                if (Minecraft.getInstance().cameraEntity instanceof Player player) {
                    if (InHandItemUtil.matchHoldZenith(player)) {
                        this.setClickCount(0);
                        cir.setReturnValue(false);
                    }
                }
            }
        } finally {
            zenith$isDown = false;
        }
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    public void onConsumeClick(CallbackInfoReturnable<Boolean> cir) {
        if (zenith$consumeClick) {
            return;
        }
        try {
            zenith$consumeClick = true;
            if (!ZenithConfigs.CLIENT_CONFIG.isLoaded() || !ZenithConfigs.ZENITH_CLIENT_CONFIG.BLOCK_INTERACTION.get()) {
                return;
            }
            KeyMapping self = (KeyMapping) (Object) this;
            Minecraft minecraft = Minecraft.getInstance();
            if (self == minecraft.options.keyUse && minecraft.player != null) {
                if (InHandItemUtil.matchHoldZenith(minecraft.player)) {
                    cir.setReturnValue(false);
                }
            }
        } finally {
            zenith$consumeClick = false;
        }
    }
}
