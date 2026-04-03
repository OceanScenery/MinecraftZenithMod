package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.registry.ZenithCapabilities;
import com.oceanscenery.zenith.registry.capabilities.IZenithDamageMark;
import net.minecraft.world.entity.Entity;

public class CapabilitiesHandler {
    public static float getMark(Entity entity){
        var tmp=entity.getCapability(ZenithCapabilities.ZENITH_MARK);
        return tmp.isPresent()?tmp.resolve().isPresent()?tmp.resolve().get().getDamageMark():0f:0f;
    }

    public static void setMark(Entity entity,float value){
        var tmp=entity.getCapability(ZenithCapabilities.ZENITH_MARK);
        if(tmp.isPresent()){
           var obj=tmp.resolve();
            obj.ifPresent(iZenithDamageMark -> iZenithDamageMark.setDamageMark(value));
        }
    }
}
