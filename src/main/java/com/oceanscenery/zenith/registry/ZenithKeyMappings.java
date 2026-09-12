package com.oceanscenery.zenith.registry;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class ZenithKeyMappings {
    public static final KeyMapping TOGGLE_ATTACK_BLACKLIST=new KeyMapping(
            "key.the_zenith_sword.attack_blacklist",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.the_zenith_sword"
    );
}
