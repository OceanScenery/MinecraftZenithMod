package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.data_component.LastUseTime;
import com.oceanscenery.zenith.mod_class.item.ZenithItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(TheZenithMod.MOD_ID);

    public static final DeferredItem<Item> ZENITH =ITEMS.registerItem(
            "zenith",
            properties -> new ZenithItem(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant().setNoRepair()
                    .component(DataComponents.TOOL,ZenithItem.createToolProperties())
                    .component(ModDataComponents.LAST_USE_TIME.get(),new LastUseTime(0))
            )
    );
}
