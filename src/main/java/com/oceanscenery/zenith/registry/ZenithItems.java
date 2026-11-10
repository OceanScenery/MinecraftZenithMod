package com.oceanscenery.zenith.registry;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.zenith_class.item.ZenithItem;
import com.oceanscenery.zenith.zenith_class.item.ZenithUpgradeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZenithItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
        ForgeRegistries.ITEMS,
        TheZenithMod.MOD_ID
    );

    public static final RegistryObject<ZenithItem> ZENITH = ITEMS.register(
        "zenith",
        () -> new ZenithItem(
            new Item.Properties().fireResistant().rarity(Rarity.EPIC).setNoRepair().stacksTo(1)
        )
    );

    public static final RegistryObject<ZenithUpgradeItem> ZENITH_UPGRADE = ITEMS.register(
        "zenith_upgrade",
        () -> new ZenithUpgradeItem(
            new Item.Properties().fireResistant().rarity(Rarity.EPIC).setNoRepair().stacksTo(1)
        )
    );
}
