package com.oceanscenery.zenith.registry;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ZenithKeyMappings {
    public static final KeyMapping TOGGLE_BLACKLIST = new KeyMapping(
        "key.the_zenith_sword.attack_blacklist",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        "key.categories.the_zenith_sword"
    );

    public static final KeyMapping TOGGLE_SLOT = new KeyMapping(
        "key.the_zenith_sword.toggle_slot_openstate",
        KeyConflictContext.UNIVERSAL,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        "key.categories.the_zenith_sword"
    );

    public static final KeyMapping OPEN_GHOST_CONFIG = new KeyMapping(
        "key.the_zenith_sword.ghost_config",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_J,
        "key.categories.the_zenith_sword"
    );
}
