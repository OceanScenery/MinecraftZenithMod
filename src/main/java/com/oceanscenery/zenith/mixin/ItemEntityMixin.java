package com.oceanscenery.zenith.mixin;

import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemEntity.class,priority = 2000)
public abstract class ItemEntityMixin {
    @Shadow
    public abstract ItemStack getItem();
    @Shadow
    private int age;

    @Inject(method = "hurtServer",at=@At("HEAD"),cancellable = true)
    public void onHurt(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir){
        ItemEntity itemEntity=(ItemEntity)(Object)this;
        if(itemEntity.getItem().is(ZenithItems.ZENITH)){
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "tick",at=@At("HEAD"))
    public void onTick(CallbackInfo ci){
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if(!itemEntity.level().isClientSide()){
            if (this.getItem().is(ZenithItems.ZENITH)) {
                this.age = 0;
            }
        }
        if (itemEntity.position().y < itemEntity.level().getMinY()-32){
            itemEntity.setPos(itemEntity.getX(),itemEntity.level().getMinY()+16,itemEntity.getZ());
        }
    }
}
