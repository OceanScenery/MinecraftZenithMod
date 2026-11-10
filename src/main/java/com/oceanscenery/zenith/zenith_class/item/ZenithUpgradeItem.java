package com.oceanscenery.zenith.zenith_class.item;

import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.registry.ZenithKeyMappings;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ZenithUpgradeItem extends Item {
    public ZenithUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            IZenithSlot slot = CapabilitiesHandler.getSlot(serverPlayer);
            if (slot != null && !slot.valid()) {
                slot.setValid(true);
                ZenithNetworkHandler.syncZenithSlot(serverPlayer);
                if (!player.getAbilities().instabuild) {
                    player.getItemInHand(hand).shrink(1);
                }
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }
        }
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> components, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, components, flag);
        DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT,
            () -> () -> components.add(
                Component.translatable(
                    "the_zenith_sword.item.zenith_upgrade.tooltip",
                    ZenithKeyMappings.TOGGLE_SLOT.getTranslatedKeyMessage(),
                    ZenithKeyMappings.OPEN_GHOST_CONFIG.getTranslatedKeyMessage()
                )
            )
        );
    }
}
