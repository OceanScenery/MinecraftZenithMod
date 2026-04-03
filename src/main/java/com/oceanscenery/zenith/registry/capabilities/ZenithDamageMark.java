package com.oceanscenery.zenith.registry.capabilities;

import net.minecraft.nbt.CompoundTag;

public class ZenithDamageMark implements IZenithDamageMark {
    private float damageMark;
    @Override
    public float getDamageMark() {
        return this.damageMark;
    }

    @Override
    public void setDamageMark(float damageMark) {
        this.damageMark = damageMark;
    }

    public CompoundTag serialize(){
        CompoundTag tag = new CompoundTag();
        tag.putFloat("zenith_damage_mark",this.damageMark);
        return tag;
    }

    public void deserialize(CompoundTag tag){
        this.damageMark = tag.getFloat("zenith_damage_mark");
    }
}
