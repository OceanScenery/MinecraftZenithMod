package com.oceanscenery.zenith.mod_class.config;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;
import org.jspecify.annotations.NonNull;

public enum InvalidDamageAction implements TranslatableEnum {
    NONE,REMOVE,DELAYED;

    @Override
    public @NonNull Component getTranslatedName() {
        return Component.translatable(
            "the_zenith_sword.configuration.invalid_damage_action.value." + this.name()
        );
    }
}
