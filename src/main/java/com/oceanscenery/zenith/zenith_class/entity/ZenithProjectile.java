package com.oceanscenery.zenith.zenith_class.entity;

import com.oceanscenery.zenith.registry.ZenithEntityDataSerializers;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ZenithProjectile extends Entity implements TraceableEntity {
    public static final EntityDataAccessor<Integer> PROGRESS = SynchedEntityData.defineId(ZenithProjectile.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Double> ANGLE = SynchedEntityData.defineId(ZenithProjectile.class, ZenithEntityDataSerializers.DOUBLE.get());
    public static final EntityDataAccessor<Double> DISTANCE = SynchedEntityData.defineId(ZenithProjectile.class, ZenithEntityDataSerializers.DOUBLE.get());
    public static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ZenithProjectile.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> SWORD_TYPE = SynchedEntityData.defineId(ZenithProjectile.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<PosUtil.Rotation> INI_ROT = SynchedEntityData.defineId(ZenithProjectile.class, ZenithEntityDataSerializers.ROTATION.get());
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(ZenithProjectile.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final int STAGE_COUNT = 20;
    public static final int MAX_SWORD_TYPE_ID = 20;
    public ItemStack weapon = ZenithItems.ZENITH.get().getDefaultInstance();
    //缓存数据,避免计算
    private Vector3[] relative = null;
    private Quaternion[] fixedPose = null;
    private float innerDamage = 0;
    private Entity cachedOwner;
    private Vec3 last_pos = null;
    private boolean to_remove;
    private int local_progress = 0;

    public ZenithProjectile(EntityType<? extends ZenithProjectile> entityType, Level level) {
        super(entityType, level);
        this.to_remove = true;
        this.noPhysics = true;
    }

    public ZenithProjectile(EntityType<? extends ZenithProjectile> entityType, Level level, @NotNull LivingEntity owner, boolean toRemove) {
        this(entityType, level);
        this.setOwner(owner);
        this.to_remove = toRemove;
        this.setIniRot(new PosUtil.Rotation(owner.getXRot(), owner.getYRot()));
    }

    public ZenithProjectile(EntityType<? extends ZenithProjectile> entityType, Level level, LivingEntity owner, boolean toRemove, double angle, int type, double distance, ItemStack weapon) {
        this(entityType, level, owner, toRemove);
        this.setAngle(angle);
        this.setSwordType(type);
        this.setDistance(distance);
        this.weapon = weapon;
    }

    public boolean shouldRemove() {
        return this.to_remove;
    }

    @Override
    public void kill() {
        if (!this.to_remove) {
            return;
        }
        super.kill();
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public @Nullable Entity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        } else if (this.getOwnerUUID() != null) {
            this.cachedOwner = LevelUtil.getEntity(this.level(), this.getOwnerUUID());
            return this.cachedOwner;
        } else {
            return null;
        }
    }

    public void setOwner(Entity owner) {
        if (owner == null) {
            this.cachedOwner = null;
            this.setOwnerID(-1);
            this.setOwnerUUID(null);
            return;
        }
        this.cachedOwner = owner;
        this.setOwnerUUID(owner.getUUID());
        this.setOwnerID(owner.getId());
    }

    public void setOwner(UUID uuid) {
        Entity found = LevelUtil.getEntity(this.level(), uuid);
        this.setOwner(found);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.equals(RemovalReason.KILLED) && !this.to_remove) {
            return;
        }
        super.remove(reason);
    }

    public UUID getOwnerUUID() {
        return this.getEntityData().get(OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(UUID other) {
        this.getEntityData().set(OWNER_UUID, Optional.ofNullable(other));
    }

    public int getSwordType() {
        return this.getEntityData().get(SWORD_TYPE);
    }

    public void setSwordType(int type) {
        this.getEntityData().set(SWORD_TYPE, type);
    }

    public int getOwnerID() {
        return this.getEntityData().get(OWNER_ID);
    }

    private void setOwnerID(int id) {
        this.getEntityData().set(OWNER_ID, id);
    }

    public int getProgress() {
        return this.getEntityData().get(PROGRESS);
    }

    public void setProgress(int progress) {
        this.getEntityData().set(PROGRESS, progress);
    }

    public double getAngle() {
        return this.getEntityData().get(ANGLE);
    }

    public void setAngle(double angle) {
        this.getEntityData().set(ANGLE, angle);
    }

    public double getDistance() {
        return this.getEntityData().get(DISTANCE);
    }

    public void setDistance(double distance) {
        this.getEntityData().set(DISTANCE, distance);
    }

    public PosUtil.Rotation getIniRot() {
        return this.getEntityData().get(INI_ROT);
    }

    public void setIniRot(PosUtil.Rotation rot) {
        this.getEntityData().set(INI_ROT, rot);
    }

    public int getLocalProgress() {
        return this.local_progress;
    }

    public float getDamage() {
        return this.innerDamage;
    }

    public void setDamage(float value) {
        this.innerDamage = value;
    }

    @Override
    protected @NotNull AABB makeBoundingBox() {
        return super.makeBoundingBox();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ANGLE, 0.0);
        this.entityData.define(DISTANCE, 20.0);
        this.entityData.define(PROGRESS, 0);
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(SWORD_TYPE, 0);
        this.entityData.define(INI_ROT, new PosUtil.Rotation(0, 0));
        this.entityData.define(OWNER_UUID, Optional.empty());
    }

    public boolean canHit(Entity entity) {
        return entity.getId() != this.getOwnerID() && !(entity instanceof ZenithProjectile);
    }

    @Override
    protected boolean canAddPassenger(@NonNull Entity passenger) {
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
        super.tick();
        if (this.getOwner() == null) {
            this.discard();
            return;
        }
        Entity owner = this.getOwner();
        if (owner == null) {
            if (!this.level().isClientSide()) {
                this.discard();
            }
            return;
        }
        int realId = owner.getId();
        if (this.getOwnerID() != realId) {
            this.setOwnerID(realId);
        }
        Vector3 pos = PosUtil.calPos(this.getDistance(), this.getProgress(), this.getAngle());
        Vector3[] reference = this.getReference();
        Vector3[] world = Vector3.WORLD;

        Vec3 realPos = pos.setZ(pos.z() - 1).VecInNewRefer(
            reference, world
        ).toVec3().add(owner.getEyePosition());

        Vec3 playerBack = new Vector3(0, 0, -1).VecInNewRefer(
            reference,
            world
        ).toVec3().add(owner.getEyePosition());

        this.setPos(playerBack);

        if (last_pos == null) {
            last_pos = realPos;
        }

        if (!this.level().isClientSide()) {
            AABB box = new AABB(realPos, last_pos).inflate(2);
            List<Entity> list = this.level().getEntitiesOfClass(Entity.class, box, this::canHit);
            for (Entity entity : list) {
                DamageHandler.applyDamage(this, entity, weapon);
            }
        }

        last_pos = realPos;

        this.setProgress(this.getProgress() + 1);
        if (this.getProgress() > STAGE_COUNT) {
            if (!this.to_remove) {
                this.to_remove = true;
            }
            this.discard();
        }
        if (this.level().isClientSide()) {
            if (this.local_progress == -1) {
                this.local_progress = this.getProgress();
            }
            this.local_progress++;
        }
    }

    @Override
    public boolean hurt(@NonNull DamageSource source, float damage) {
        return false;
    }

    private void invalidateCache() {
        this.fixedPose = null;
        this.relative = null;
        this.cachedOwner = null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
        this.invalidateCache();
        CompoundTag child = input.getCompound("Zenith");
        this.setAngle(child.getDouble("Angle"));
        this.setProgress(Mth.clamp(child.getInt("Age"), 0, 21));
        try {
            this.setOwner(UUID.fromString(child.getString("Owner")));
        } catch (Exception e) {
        }
        this.setSwordType(Mth.clamp(child.getInt("SwordType"), 0, MAX_SWORD_TYPE_ID));
        ListTag rot = child.getList("Rotation", CompoundTag.TAG_DOUBLE);
        try {
            this.setIniRot(new PosUtil.Rotation(rot.getDouble(0), rot.getDouble(1)));
        } catch (Exception e) {
            this.setIniRot(new PosUtil.Rotation(0, 0));
        }
        this.innerDamage = child.getFloat("StoredDamage");
        this.setDistance(Mth.clamp(child.getDouble("FlyDistance"), 1, 100));
        this.to_remove = child.getBoolean("ShouldBeRemoved");
        if (this.weapon == null) {
            this.weapon = ZenithItems.ZENITH.get().getDefaultInstance();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        CompoundTag child = new CompoundTag();
        output.put("Zenith", child);
        child.putDouble("Angle", this.getAngle());
        child.putInt("Age", this.getProgress());
        if (this.getOwnerUUID() != null) {
            child.putString("Owner", this.getOwnerUUID().toString());
        }
        child.putInt("SwordType", this.getSwordType());
        ListTag rot = newDoubleList(
            this.getIniRot().getPitch(),
            this.getIniRot().getYaw()
        );
        child.put(
            "Rotation",
            rot
        );
        child.putFloat("StoredDamage", this.innerDamage);
        child.putDouble("FlyDistance", this.getDistance());
        child.putBoolean("ShouldBeRemoved", this.to_remove);
    }

    public Vector3[] getReference() {
        if (this.relative == null) {
            this.relative = Vector3.getReferFromAngle(this.getIniRot().getPitch(), this.getIniRot().getYaw());
        }
        return this.relative;
    }

    public Quaternion[] getFixedPose() {
        if (this.fixedPose == null) {
            this.fixedPose = new Quaternion[3];
            this.fixedPose[0] = Quaternion.trans(new Vector3(0, 0, 1), new Vector3(this.getReference()[2].x(), 0, this.getReference()[2].z()));
            this.fixedPose[1] = Quaternion.trans(new Vector3(this.getReference()[2].x(), 0, this.getReference()[2].z()), this.getReference()[2]);
            this.fixedPose[2] = Quaternion.rotate(this.getReference()[2], this.getAngle());
        }
        return this.fixedPose;
    }
}
