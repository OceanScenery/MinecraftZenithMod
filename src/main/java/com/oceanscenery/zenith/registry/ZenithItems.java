package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.mod_class.data_component.Distance;
import com.oceanscenery.zenith.mod_class.data_component.LastUseTime;
import com.oceanscenery.zenith.mod_class.item.ZenithItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.providers.EnchantmentProviderTypes;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZenithItems {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(TheZenithMod.MOD_ID);

    public static final DeferredItem<Item> ZENITH =ITEMS.registerItem(
            "zenith",
            properties -> new ZenithItem(properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant().setNoCombineRepair()
                    .component(DataComponents.TOOL,ZenithItem.createToolProperties())
                    .component(ZenithDataComponents.LAST_USE_TIME.get(),new LastUseTime(0))
                    .component(ZenithDataComponents.DISTANCE.get(),new Distance(20))
                    .component(ZenithDataComponents.ATTACK_MODE.get(),new AttackMode(AttackMode.Mode.LIVING_ENTITY,true))
                    .component(DataComponents.ENCHANTABLE,new Enchantable(50))
            )
    );
}
