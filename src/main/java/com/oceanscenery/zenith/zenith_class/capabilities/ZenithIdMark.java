package com.oceanscenery.zenith.zenith_class.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ZenithIdMark implements IZenithIdMark{
    Set<String> idSet=new HashSet<>();

    @Override
    public Set<String> getIdSet() {
        return idSet;
    }

    public CompoundTag serialize(){
        CompoundTag result=new CompoundTag();
        ListTag tags=new ListTag();
        for(String str:new ArrayList<>(idSet)){
            tags.add(StringTag.valueOf(str));
        }
        result.put("ZenithIdMark",tags);
        return result;
    }

    public void deserialize(CompoundTag tag){
        Tag aTag=tag.get("ZenithIdMark");
        idSet.clear();
        if(aTag instanceof ListTag listTag){
            for(int i=0;i<listTag.size();i++){
                String str=listTag.getString(i);
                idSet.add(str);
            }
        }
    }
}
