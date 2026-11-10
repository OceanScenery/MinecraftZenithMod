package com.oceanscenery.zenith.mod_class.item;

import com.oceanscenery.zenith.mod_class.attachment.ZenithSlot;
import com.oceanscenery.zenith.registry.ZenithAttachments;
import com.oceanscenery.zenith.util.AttachmentUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class ZenithUpgradeItem extends Item {
    public ZenithUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ZenithSlot slot = AttachmentUtil.getSlot(player);
        if (slot.valid()) {
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer) {
            player.setData(
                ZenithAttachments.ZENITH_SLOT,
                ZenithSlot.Builder.of()
                    .valid(true).build()
            );
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.success(stack);
    }
}
