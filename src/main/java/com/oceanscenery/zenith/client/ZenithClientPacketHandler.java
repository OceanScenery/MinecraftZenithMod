package com.oceanscenery.zenith.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZenithClientPacketHandler {
    public static void handleEntityInfPacket(int entityId,boolean inList){
        RenderEntityTip.serverBackId=entityId;
        RenderEntityTip.canAttack=!inList;
    }
}
