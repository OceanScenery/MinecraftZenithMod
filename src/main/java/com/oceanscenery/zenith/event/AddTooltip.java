package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.mod_class.data_component.Distance;
import com.oceanscenery.zenith.registry.ZenithDataComponents;
import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid=TheZenithMod.MOD_ID)
public class AddTooltip {
    @SubscribeEvent
    public static void addTooltip(ItemTooltipEvent event) {
        TooltipFlag tooltipFlag=event.getFlags();
        boolean advanced=tooltipFlag.isAdvanced();
        ItemStack stack=event.getItemStack();
        if(!stack.is(ZenithItems.ZENITH)){
            return;
        }
        List<Component> tooltipComponents=event.getToolTip();
        if(stack.get(ZenithDataComponents.DISTANCE)==null){
            stack.set(ZenithDataComponents.DISTANCE,new Distance(20));
        }
        if(stack.get(ZenithDataComponents.ATTACK_MODE)==null){
            stack.set(ZenithDataComponents.ATTACK_MODE,new AttackMode(AttackMode.Mode.LIVING_ENTITY,true));
        }
        double dist=stack.get(ZenithDataComponents.DISTANCE).dist();
        String atk=stack.get(ZenithDataComponents.ATTACK_MODE).getStrMode();
        boolean atkP=stack.get(ZenithDataComponents.ATTACK_MODE).attackPlayer();
        if(advanced && tooltipComponents.size()>=2){
            int i=tooltipComponents.size();
            tooltipComponents.add(i-2,Component.translatable("the_zenith_sword.item.attack_player_mode.tooltip.post"));
            tooltipComponents.add(i-2,Component.translatable("the_zenith_sword.item.attack_player_mode.tooltip.pre").append(":" + atkP));
            tooltipComponents.add(i-2,Component.translatable("the_zenith_sword.item.attack_mode.tooltip_post"));
            tooltipComponents.add(i-2,Component.translatable("the_zenith_sword.item.attack_mode.tooltip_pre").append(":" + atk));
            tooltipComponents.add(i-2,Component.translatable("the_zenith_sword.item.distance.tooltip_post"));
            tooltipComponents.add(i-2,Component.translatable("the_zenith_sword.item.distance.tooltip_pre").append(":" + dist));
            return;
        }
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.distance.tooltip_pre").append(":" + dist));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.distance.tooltip_post"));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_mode.tooltip_pre").append(":" + atk));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_mode.tooltip_post"));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_player_mode.tooltip.pre").append(":" + atkP));
        tooltipComponents.add(Component.translatable("the_zenith_sword.item.attack_player_mode.tooltip.post"));
    }
}
