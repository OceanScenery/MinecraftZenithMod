package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.util.Quaternion;
import com.oceanscenery.zenith.util.Vector3;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;

public class ZenithProjectileRenderState extends EntityRenderState {
    public int swordType;
    public int actuallyProgress;
    public double distance;
    public double angle;
    public Vector3[] reference;
    public int ownerId;
    public ClientLevel level;
    public Quaternion[] fixed;
    Vector3 render_pos;
    Vector3 round;
    public ItemStackRenderState itemState=new ItemStackRenderState();

    public Entity getOwner(){
        return level.getEntity(ownerId);
    }

    public ZenithProjectileRenderState(
            int swordType,int local_progress,double distance,double angle,int ownerId
    ){
        this.swordType=swordType;
        this.actuallyProgress=local_progress;
        this.distance=distance;
        this.angle=angle;
        this.ownerId=ownerId;
        this.level=null;
        this.itemState=null;
        this.reference=null;
        this.fixed=null;
        this.round=null;
    }
}
