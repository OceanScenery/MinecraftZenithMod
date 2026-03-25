package com.oceanscenery.zenith.mod_class.entity;

import com.oceanscenery.zenith.event.DamageHandle;
import com.oceanscenery.zenith.mod_class.config.ZenithConfig;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithEntityDataSerializer;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.tool.PosUtil;
import com.oceanscenery.zenith.tool.Vector3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ZenithProjectile extends Entity implements TraceableEntity {

    public static final EntityDataAccessor<Vector3> FACING_VECTOR=SynchedEntityData.defineId(ZenithProjectile.class, ZenithEntityDataSerializer.VECTOR.get());
    public static final EntityDataAccessor<Vector3> LAST_VECTOR=SynchedEntityData.defineId(ZenithProjectile.class, ZenithEntityDataSerializer.VECTOR.get());
    public static final EntityDataAccessor<Integer> PROGRESS=SynchedEntityData.defineId(ZenithProjectile.class,EntityDataSerializers.INT);
    public static final EntityDataAccessor<Double> ANGLE=SynchedEntityData.defineId(ZenithProjectile.class, ZenithEntityDataSerializer.DOUBLE.get());
    public static final EntityDataAccessor<Double> DISTANCE=SynchedEntityData.defineId(ZenithProjectile.class, ZenithEntityDataSerializer.DOUBLE.get());
    public static final EntityDataAccessor<Integer> OWNER_ID =SynchedEntityData.defineId(ZenithProjectile.class,EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> SWORD_TYPE =SynchedEntityData.defineId(ZenithProjectile.class,EntityDataSerializers.INT);
    public static final EntityDataAccessor<PosUtil.Rotation> INI_ROT=SynchedEntityData.defineId(ZenithProjectile.class, ZenithEntityDataSerializer.ROTATION.get());

    private LivingEntity owner;
    private UUID owner_uuid;
    private Vec3 center_pos=null;
    private Vec3 last_pos=null;
    private boolean to_remove;
    public static final int STAGE_COUNT=20;
    private int local_progress=0;
    public ItemStack weapon=ZenithItems.ZENITH.toStack();

    public ZenithProjectile(EntityType<? extends ZenithProjectile> entityType, Level level) {
        super(entityType, level);
        noCulling=true;
        to_remove=true;
        this.noPhysics=true;
        this.getEntityData().set(FACING_VECTOR,new Vector3(0,0,1));
    }

    public ZenithProjectile(EntityType<? extends ZenithProjectile> entityType, Level level, @NotNull LivingEntity owner, boolean to_remove){
        this(entityType,level);
        this.owner=owner;
        this.owner_uuid=owner.getUUID();
        this.to_remove=to_remove;
        this.setOwnerID(this.getOwner()==null?-1:this.getOwner().getId());
        this.setIniRot(new PosUtil.Rotation(owner.getXRot(),owner.getYRot()));
    }

    public ZenithProjectile(EntityType<? extends ZenithProjectile> entityType,Level level,LivingEntity owner,boolean to_remove,double angle,int type,double distance,ItemStack weapon){
        this(entityType,level,owner,to_remove);
        this.setAngle(angle);
        this.setSwordType(type);
        this.setDistance(distance);
        this.center_pos=owner.getEyePosition().relative(owner.getDirection(),(distance-1)/2);
        setFacingVector(new Vector3(0,0,1));
        this.weapon=weapon;
    }

    @Override
    public void kill() {
        super.kill();
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    public @Nullable Entity getOwner() {
        if (this.owner != null && !this.owner.isRemoved()) {
            return this.owner;
        } else if (this.owner_uuid != null && this.level() instanceof ServerLevel serverlevel) {
            this.owner = serverlevel.getPlayerByUUID(this.owner_uuid);
            return this.owner;
        } else {
            return null;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if(reason.equals(RemovalReason.KILLED) && !this.to_remove){
            return;
        }
        super.remove(reason);
    }

    public UUID getOwnerUuid(){
        return this.owner_uuid;
    }

    public int getSwordType(){
        return this.getEntityData().get(SWORD_TYPE);
    }

    public void setSwordType(int type){
        this.getEntityData().set(SWORD_TYPE,type);
    }

    public int getOwnerID(){
        return this.getEntityData().get(OWNER_ID);
    }

    public Vector3 getLastVector(){
        return this.getEntityData().get(LAST_VECTOR);
    }

    public Vector3 getFacingVector(){
        return this.getEntityData().get(FACING_VECTOR);
    }

    public int getProgress(){
        return this.getEntityData().get(PROGRESS);
    }

    public double getAngle(){
        return this.getEntityData().get(ANGLE);
    }

    public double getDistance(){
        return this.getEntityData().get(DISTANCE);
    }

    public PosUtil.Rotation getIniRot(){
        return this.getEntityData().get(INI_ROT);
    }

    public void setIniRot(PosUtil.Rotation rot){
        this.getEntityData().set(INI_ROT,rot);
    }

    private void setOwnerID(int id){
        this.getEntityData().set(OWNER_ID,id);
    }

    public void setLastVector(Vector3 vec){
        this.getEntityData().set(LAST_VECTOR,vec);
    }

    public void setFacingVector(Vector3 vec){
        this.getEntityData().set(FACING_VECTOR,vec);
    }

    public void setProgress(int progress){
        this.getEntityData().set(PROGRESS,progress);
    }

    public void setAngle(double angle){
        this.getEntityData().set(ANGLE,angle);
    }

    public void setDistance(double distance){
        this.getEntityData().set(DISTANCE,distance);
    }

    public int getLocalProgress(){
        return this.local_progress;
    }

    public void setLocalProgress(int value){
        this.local_progress=value;
    }

    @Override
    protected @NotNull AABB makeBoundingBox() {
        return new AABB(this.getX(),this.getY(),this.getZ(),this.getX(),this.getY(),this.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FACING_VECTOR,new Vector3(0,0,1));
        builder.define(LAST_VECTOR,new Vector3(0,0,1));
        builder.define(ANGLE, 0.0);
        builder.define(DISTANCE,20.0);
        builder.define(PROGRESS,0);
        builder.define(OWNER_ID,-1);
        builder.define(SWORD_TYPE,0);
        builder.define(INI_ROT,new PosUtil.Rotation(0,0));
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compound) {
    }

    public boolean canHit(Entity entity){
        if(entity.getId()==this.getOwnerID() || entity instanceof ZenithProjectile){
            return false;
        }
        return true;
    }

    @Override
    public boolean canChangeDimensions(@NotNull Level oldLevel, @NotNull Level newLevel) {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        if(!this.level().isClientSide){
            super.tick();
            if (this.owner_uuid == null) {
                return;
            }
            if (this.getOwner() == null) {
                this.discard();
                return;
            }
            if(this.getOwnerID()==-1){
                this.setOwnerID(this.getOwner().getId());
            }
            Vector3 pos = PosUtil.calPos(this.getDistance(), this.getProgress(), this.getAngle());
            Vector3 mark_center = new Vector3(0, 0, this.getDistance()/2);
            Vector3[] reference = Vector3.getReferFromAngle(this.getIniRot().getPitch(),this.getIniRot().getYaw());
            Vector3[] world=new Vector3[]{new Vector3(1,0,0),new Vector3(0,1,0),new Vector3(0,0,1)};

            Vec3 real_pos = pos.setZ(pos.getZ()-1).VecInNewRefer(
                    reference,world
            ).toVec3().add(this.owner.getEyePosition());

            Vec3 real_center = mark_center.setZ((mark_center.getZ() - 1)).VecInNewRefer(
                    reference,world
            ).toVec3().add(this.owner.getEyePosition());

            Vec3 player_back=new Vector3(0,0,-1).VecInNewRefer(
                    reference,
                    world
            ).toVec3().add(this.owner.getEyePosition());

            this.setPos(player_back);

            if(last_pos==null){
                last_pos=real_pos;
            }

            AABB box=new AABB(real_pos,last_pos).inflate(2);
            List<Entity> list=this.level().getEntitiesOfClass(Entity.class,box,this::canHit);
            for(Entity entity:list){
                DamageHandle.applyDamage(owner,entity,weapon);
            }

            last_pos=real_pos;

            if(this.getProgress() > STAGE_COUNT && !this.to_remove) {
                this.to_remove=true;
            }

            if (this.center_pos == null) {
                this.discard();
                return;
            }
            this.center_pos = real_center;
            this.setFacingVector(Vector3.transToVector3(real_center.subtract(real_pos)));

            this.setLastVector(this.getFacingVector());
            this.setProgress(this.getProgress() + 1);
            if (this.getProgress() > STAGE_COUNT) {
                this.discard();
            }


        }else{
            if(local_progress==-1){
                local_progress=getProgress();
            }
            local_progress++;
        }
    }
}
