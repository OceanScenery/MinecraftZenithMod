package com.oceanscenery.zenith.mixin;

import com.oceanscenery.zenith.mixin_extension.IZenithEntityExtension;
import com.oceanscenery.zenith.util.StaticCollections;
import com.oceanscenery.zenith.zenith_class.entity.ZenithProjectile;
import com.oceanscenery.zenith.zenith_class.saved_data.RemovedEntitiesData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(value = Entity.class, priority = Integer.MAX_VALUE)
public abstract class EntityMixin implements IZenithEntityExtension {
    @Shadow
    @Nullable
    private Entity.RemovalReason removalReason;

    @Shadow
    private Level level;

    @Shadow
    protected UUID uuid;
    @Unique
    private volatile boolean zenith$allReturn;
    @Unique
    private boolean zenith$removalInProgress;

    @Override
    public final boolean zenith$isAllReturn() {
        if (StaticCollections.REMOVE_MARK.get().contains(this) || this.zenith$removalInProgress || this.uuid == null) {
            return false;
        }
        if ((Object) this instanceof Player) {
            return false;
        }
        try {
            StaticCollections.REMOVE_MARK.get().add((Entity) (Object) this);
            if (this.zenith$allReturn) {
                return true;
            } else if (RemovedEntitiesData.contains(this.level, this.uuid)) {
                return (this.zenith$allReturn = true);
            }
            return false;
        } finally {
            StaticCollections.REMOVE_MARK.get().remove(this);
        }
    }

    @Override
    public final void zenith$setAllReturn(boolean enabled) {
        this.zenith$allReturn = enabled;
    }

    @Override
    public boolean zenith$inRemoval() {
        return this.zenith$removalInProgress;
    }

    @Override
    public void zenith$setInRemoval(boolean value) {
        this.zenith$removalInProgress = value;
    }

    @Override
    public UUID zenith$rawId() {
        return this.uuid;
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void onSetRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof ZenithProjectile zenithProjectile) {
            if (!zenithProjectile.shouldRemove()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof ZenithProjectile zenithProjectile) {
            if (!zenithProjectile.shouldRemove()) {
                ci.cancel();
            }
        }
    }
}
