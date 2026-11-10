package com.oceanscenery.zenith.zenith_class.capabilities.cap_interface;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.function.Function;

public interface IZenithSlot {
    /**
     * 返回玩家是否有权使用
     */
    boolean valid();

    /**
     * 返回玩家是否使用
     */
    boolean open();

    void setValid(boolean value);

    /**
     * 切换玩家的使用状态
     * @param change 根据已有状态选择是开启还是关闭
     */
    void toggle(Function<Boolean, Boolean> change);

    /**
     * 切换玩家的使用状态为当前的相反状态
     */
    void toggle();

    /**
     * 获取开启时生成的影子物品
     */
    ItemStack get();

    /**
     * 影子物品的默认索敌距离，默认 20
     */
    double getDistance();

    /**
     * 影子物品的默认索敌距离
     */
    void setDistance(double distance);

    /**
     * 影子物品的攻击模式（0 : 仅生物，1 : 可攻击生物，2 : 全部实体），默认 0。
     */
    int getMode();

    /**
     * 设置影子物品的攻击模式。
     */
    void setMode(int mode);

    void discardCache();

    Player getOwner();
}
