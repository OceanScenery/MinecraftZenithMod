package com.oceanscenery.zenith.hooks;

import com.oceanscenery.zenith.mixin_extension.IZenithEntityExtension;

public final class EntityMarkHook {
    private EntityMarkHook() {}

    public static boolean marked(Object value) {
        return IZenithEntityExtension.marked(value);
    }
}
