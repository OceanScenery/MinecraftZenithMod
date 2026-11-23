package com.oceanscenery.zenith.mixin;

import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import com.oceanscenery.zenith.util.ConfigUtil;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(LootPool.class)
public class LootPoolMixin {
    @Shadow
    @Final
    @Mutable
    private Predicate<LootContext> compositeCondition;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void zenith$init$tail(LootPoolEntryContainer[] containers, LootItemCondition[] conditions, LootItemFunction[] functions, NumberProvider provider1, NumberProvider provider2, String name, CallbackInfo ci) {
        var origin = this.compositeCondition;
        this.compositeCondition = context -> {
            var source = context.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
            if (source != null && ConfigUtil.forceRemoval() && (source.is(ZenithDamageTypes.ZENITH) || source.is(ZenithDamageTypes.ZENITH_KNOCKBACK))) {
                return true;
            }
            return origin.test(context);
        };
    }
}
