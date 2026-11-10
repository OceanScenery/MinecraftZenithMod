package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.registry.ZenithKeyMappings;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID, value = Dist.CLIENT)
public final class ClientZenithSlot {
    private ClientZenithSlot() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(ZenithItems.ZENITH_UPGRADE)) {
            event.getToolTip().add(Component.translatable("the_zenith_sword.item.zenith_upgrade.tooltip",
                ZenithKeyMappings.TOGGLE_SLOT.getTranslatedKeyMessage(),
                ZenithKeyMappings.OPEN_GHOST_CONFIG.getTranslatedKeyMessage()));
        }
    }
}
