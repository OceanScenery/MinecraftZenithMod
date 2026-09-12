package com.oceanscenery.zenith.registry;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

public class ZenithKeyMappings {
    public static final KeyMapping TOGGLE_BLACKLIST=new KeyMapping(
            "key.the_zenith_sword.attack_blacklist",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.the_zenith_sword"
    );
}
