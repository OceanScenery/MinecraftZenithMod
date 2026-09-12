package com.oceanscenery.zenith.zenith_class.capabilities;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ZenithUuidMark implements IZenithUuidMark{
    Set<UUID> uuidSet=new HashSet<>();

    @Override
    public Set<UUID> getUuidSet() {
        return uuidSet;
    }

    public CompoundTag serialize(){
        CompoundTag result=new CompoundTag();
        ListTag tags=new ListTag();
        for(UUID id:new ArrayList<>(uuidSet)){
            tags.add(StringTag.valueOf(id.toString()));
        }
        result.put("ZenithUuidMark",tags);
        return result;
    }

    public void deserialize(CompoundTag tag){
        Tag aTag=tag.get("ZenithUuidMark");
        uuidSet.clear();
        if(aTag instanceof ListTag listTag){
            try{
                for (int i = 0; i < listTag.size(); i++) {
                    String str = listTag.getString(i);
                    uuidSet.add(UUID.fromString(str));
                }
            }catch (IllegalArgumentException e){
                TheZenithMod.LOGGER.error("Error in loading uuid:",e);
                uuidSet.clear();
            }
        }
    }
}
