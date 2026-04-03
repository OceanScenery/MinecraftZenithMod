package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class WeaponHandler {
    public static void checkExist(CompoundTag tag){
        if(!tag.contains("Zenith")){
            CompoundTag z_tag=new CompoundTag();
            z_tag.putLong("use_tick",0);
            z_tag.putDouble("distance",20);
            z_tag.putInt("attack_mode",0);
            z_tag.putBoolean("attack_player",true);
            tag.put("Zenith",z_tag);
        }
    }

    public static int getAttackMode(ItemStack item){
        if(!item.is(ZenithItems.ZENITH.get())){
            return 0;
        }
        CompoundTag tag=item.getOrCreateTag();
        checkExist(tag);
        CompoundTag z_tag=tag.getCompound("Zenith");
        if(!z_tag.contains("attack_mode")){
            z_tag.putInt("attack_mode",0);
            return 0;
        }
        return z_tag.getInt("attack_mode");
    }

    public static long getLastUseTick(ItemStack item){
        if(!item.is(ZenithItems.ZENITH.get())){
            return 0;
        }
        CompoundTag tag=item.getOrCreateTag();
        checkExist(tag);
        CompoundTag z_tag=tag.getCompound("Zenith");
        if(!z_tag.contains("use_tick")){
            z_tag.putLong("use_tick",0);
            return 0;
        }
        return z_tag.getLong("use_tick");
    }

    public static double getDistance(ItemStack item){
        if(!item.is(ZenithItems.ZENITH.get())){
            return 20;
        }
        CompoundTag tag=item.getOrCreateTag();
        checkExist(tag);
        CompoundTag z_tag=tag.getCompound("Zenith");
        if(!z_tag.contains("distance")){
            z_tag.putDouble("distance",20);
            return 20;
        }
        return z_tag.getDouble("distance");
    }

    public static boolean attackPlayer(ItemStack item){
        if(!item.is(ZenithItems.ZENITH.get())){
            return true;
        }
        CompoundTag tag=item.getOrCreateTag();
        checkExist(tag);
        CompoundTag z_tag=tag.getCompound("Zenith");
        if(!z_tag.contains("attack_player")){
            z_tag.putBoolean("attack_player",true);
            return true;
        }
        return z_tag.getBoolean("attack_player");
    }
}
