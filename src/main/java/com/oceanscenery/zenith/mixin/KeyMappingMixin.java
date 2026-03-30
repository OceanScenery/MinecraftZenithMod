package com.oceanscenery.zenith.mixin;

import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=KeyMapping.class,priority = 114514)
public abstract class KeyMappingMixin {
    @Accessor(value = "clickCount")
    abstract void setClickCount(int value);
    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    public void onConsumeClick(CallbackInfoReturnable<Boolean> cir) {
        if(!ZenithConfigs.ZENITH_CLIENT_CONFIG.BLOCK_INTERACTION.get()){
            return;
        }
        KeyMapping self = (KeyMapping)(Object)this;
        Minecraft minecraft = Minecraft.getInstance();
        if (self == minecraft.options.keyUse && minecraft.player != null) {
            if (minecraft.player.getMainHandItem().is(ZenithItems.ZENITH) || minecraft.player.getOffhandItem().is(ZenithItems.ZENITH)) {
                this.setClickCount(0);
                cir.setReturnValue(false);
            }
        }
    }
}
