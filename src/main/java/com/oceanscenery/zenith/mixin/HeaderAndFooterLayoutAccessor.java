package com.oceanscenery.zenith.mixin;

import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = HeaderAndFooterLayout.class)
public interface HeaderAndFooterLayoutAccessor {
    @Accessor(value = "headerFrame")
    public FrameLayout getHeaderFrame();
}
