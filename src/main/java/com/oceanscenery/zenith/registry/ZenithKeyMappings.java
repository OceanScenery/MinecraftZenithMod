package com.oceanscenery.zenith.registry;

import com.mojang.blaze3d.platform.InputConstants;
import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ZenithKeyMappings {
    public static final KeyMapping TOGGLE_ATTACK_BLACKLIST=new KeyMapping(
            "key.the_zenith_sword.attack_blacklist",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID,"main"))
    );
}
