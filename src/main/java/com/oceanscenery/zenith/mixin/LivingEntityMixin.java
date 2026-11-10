package com.oceanscenery.zenith.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.oceanscenery.zenith.mixin_extension.IZenithLivingEntityExtension;
import com.oceanscenery.zenith.mod_class.attachment.ZenithSlot;
import com.oceanscenery.zenith.registry.ZenithAttachments;
import com.oceanscenery.zenith.util.ConfigUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(value = LivingEntity.class, priority = 150000)
public abstract class LivingEntityMixin implements IZenithLivingEntityExtension {
    @Unique
    private int zenith$deathTime = 0;
    @Unique
    private boolean zenith$dead = false;

    @ModifyReturnValue(method = "getAttributeValue", at = @At("RETURN"))
    private double zenith$getAttributeValue(double original, @Local(argsOnly = true) Holder<Attribute> attribute) {
        //noinspection ConstantValue
        if (!((Object) this instanceof Player player)) {
            return original;
        }
        boolean open = Optional.ofNullable(player.getExistingDataOrNull(ZenithAttachments.ZENITH_SLOT))
            .filter(ZenithSlot::open).isPresent();

        if (open) {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) {
                return ConfigUtil.defaultDamage();
            }

            if (attribute.equals(Attributes.ATTACK_SPEED)) {
                return ConfigUtil.attackSpeed(true);
            }
        }
        return original;
    }

    @Override
    public int zenith$deathTime() {
        return this.zenith$deathTime;
    }

    @Override
    public void zenith$setDeathTime(int value) {
        this.zenith$deathTime = value;
    }

    @Override
    public boolean zenith$deadMark() {
        return this.zenith$dead;
    }

    @Override
    public void zenith$setDead(boolean value) {
        this.zenith$dead = value;
        if (!value) {
            this.zenith$deathTime = 0;
        }
    }
}
