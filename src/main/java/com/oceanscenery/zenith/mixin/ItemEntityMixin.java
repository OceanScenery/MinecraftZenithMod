package com.oceanscenery.zenith.mixin;

import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = ItemEntity.class, priority = 2000)
public abstract class ItemEntityMixin {
    @Shadow
    public abstract ItemStack getItem();

    @Final
    @Shadow
    private static int INFINITE_PICKUP_DELAY;
    @Shadow
    private int pickupDelay;
    @Shadow
    private UUID thrower;
    @Shadow
    private int age;

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if (itemEntity.getItem().is(ZenithItems.ZENITH)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if (!itemEntity.level().isClientSide) {
            if (this.getItem().is(ZenithItems.ZENITH) && this.pickupDelay != INFINITE_PICKUP_DELAY) {
                this.age = 0;
            }
        }
        if (itemEntity.position().y < itemEntity.level().getMinBuildHeight() - 32) {
            itemEntity.setPos(itemEntity.getX(), itemEntity.level().getMinBuildHeight() + 16, itemEntity.getZ());
            itemEntity.setNoGravity(true);
            itemEntity.setGlowingTag(true);
            if (itemEntity.level() instanceof ServerLevel serverLevel) {
                Entity thrower = serverLevel.getEntity(this.thrower);
                if (thrower instanceof Player player) {
                    itemEntity.setNoPickUpDelay();
                    itemEntity.playerTouch(player);
                }
            }
        }
    }
}
