package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.mod_class.data_component.Distance;
import com.oceanscenery.zenith.mod_class.data_component.LastUseTime;
import com.oceanscenery.zenith.mod_class.item.ZenithItem;
import com.oceanscenery.zenith.mod_class.item.ZenithUpgradeItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantable;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZenithItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheZenithMod.MOD_ID);

    public static final DeferredItem<ZenithItem> ZENITH = ITEMS.registerItem(
        "zenith",
        properties -> new ZenithItem(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant().setNoCombineRepair()
            .component(DataComponents.TOOL, ZenithItem.createToolProperties())
            .component(ZenithDataComponents.LAST_USE_TIME.get(), new LastUseTime(0))
            .component(ZenithDataComponents.DISTANCE.get(), new Distance(20))
            .component(ZenithDataComponents.ATTACK_MODE.get(), AttackMode.LIVING_ENTITY)
            .component(DataComponents.ENCHANTABLE, new Enchantable(50))
        )
    );

    public static final DeferredItem<ZenithUpgradeItem> ZENITH_UPGRADE = ITEMS.registerItem(
        "zenith_upgrade",
        properties -> new ZenithUpgradeItem(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant().setNoCombineRepair())
    );
}
