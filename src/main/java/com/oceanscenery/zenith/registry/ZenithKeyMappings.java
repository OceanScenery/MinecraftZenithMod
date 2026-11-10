package com.oceanscenery.zenith.registry;

import com.mojang.blaze3d.platform.InputConstants;
import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public class ZenithKeyMappings {
    public static final Identifier CATEGORY_ID =
        Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID, "main");
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(CATEGORY_ID);

    public static final KeyMapping TOGGLE_ATTACK_BLACKLIST = new KeyMapping(
        "key.the_zenith_sword.attack_blacklist",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYBOARD,
        InputConstants.KEY_V,
        CATEGORY
    );

    public static final KeyMapping TOGGLE_SLOT = new KeyMapping(
        "key.the_zenith_sword.toggle_slot_openstate",
        KeyConflictContext.UNIVERSAL,
        InputConstants.Type.KEYBOARD,
        InputConstants.KEY_K,
        CATEGORY
    );

    public static final KeyMapping OPEN_GHOST_CONFIG = new KeyMapping(
        "key.the_zenith_sword.ghost_config",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYBOARD,
        InputConstants.KEY_J,
        CATEGORY
    );
}
