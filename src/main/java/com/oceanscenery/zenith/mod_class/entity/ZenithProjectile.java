package com.oceanscenery.zenith.mod_class.entity;

import com.oceanscenery.zenith.registry.ZenithEntityDataSerializer;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.util.DamageHandler;
import com.oceanscenery.zenith.util.PosUtil;
import com.oceanscenery.zenith.util.Quaternion;
import com.oceanscenery.zenith.util.Vector3;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ZenithProjectile extends Entity implements TraceableEntity {
    public static final EntityDataAccessor<Integer> PROGRESS = SynchedEntityData.defineId(ZenithProjectile.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Double> ANGLE = SynchedEntityData.defineId(ZenithProjectile.class, ZenithEntityDataSerializer.DOUBLE.get());
    public static final EntityDataAccessor<Double> DISTANCE = SynchedEntityData.defineId(ZenithProjectile.class, ZenithEntityDataSerializer.DOUBLE.get());
    public static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(ZenithProjectile.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> SWORD_TYPE = SynchedEntityData.defineId(ZenithProjectile.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<PosUtil.Rotation> INI_ROT = SynchedEntityData.defineId(ZenithProjectile.class, ZenithEntityDataSerializer.ROTATION.get());
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(ZenithProjectile.class, ZenithEntityDataSerializer.OPTIONAL_UUID.get());
    public static final int STAGE_COUNT = 20;
    public static final int MAX_SWORD_TYPE_ID = 20;
    public ItemStack weapon = ZenithItems.ZENITH.toStack();
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
    public void kill(@NonNull ServerLevel level) {
        if (!this.to_remove) {
            return;
        }
        super.kill(level);
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IMMOVEABLE;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    public @Nullable Entity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        } else if (this.getOwnerUUID() != null) {
            this.cachedOwner = this.level().getEntity(this.getOwnerUUID());
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
        Entity found = this.level().getEntity(uuid);
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
    protected @NonNull AABB makeBoundingBox(@NonNull Vec3 position) {
        return super.makeBoundingBox(position);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ANGLE, 0.0);
        builder.define(DISTANCE, 20.0);
        builder.define(PROGRESS, 0);
        builder.define(OWNER_ID, -1);
        builder.define(SWORD_TYPE, 0);
        builder.define(INI_ROT, new PosUtil.Rotation(0, 0));
        builder.define(OWNER_UUID, Optional.empty());
    }

    public boolean canHit(Entity entity) {
        return entity.getId() != this.getOwnerID() && !(entity instanceof ZenithProjectile);
    }

    @Override
    protected boolean canAddPassenger(@NonNull Entity passenger) {
        return false;
    }

    @Override
    public boolean canTeleport(@NonNull Level from, @NonNull Level to) {
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

        Vec3 realPos = pos.setZ(pos.getZ() - 1).VecInNewRefer(
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
    public boolean hurtServer(@NonNull ServerLevel level, @NonNull DamageSource source, float damage) {
        return false;
    }

    private void invalidateCache() {
        this.fixedPose = null;
        this.relative = null;
        this.cachedOwner = null;
    }

    @Override
    protected void readAdditionalSaveData(@NonNull ValueInput input) {
        this.invalidateCache();
        ValueInput child = input.childOrEmpty("Zenith");
        this.setAngle(child.getDoubleOr("Angle", 0.0));
        this.setProgress(Math.clamp(child.getIntOr("Age", 0), 0, 21));
        this.setOwner(child.read("Owner", UUIDUtil.CODEC).orElse(null));
        this.setSwordType(Math.clamp(child.getIntOr("SwordType", 0), 0, MAX_SWORD_TYPE_ID));
        this.setIniRot(child.read("Rotation", PosUtil.Rotation.CODEC).orElse(new PosUtil.Rotation(0, 0)));
        this.innerDamage = child.getFloatOr("StoredDamage", 10.0f);
        this.setDistance(child.getDoubleOr("FlyDistance", 20.0f));
        this.to_remove = child.getBooleanOr("ShouldBeRemoved", false);
        if (this.weapon == null) {
            this.weapon = ZenithItems.ZENITH.toStack();
        }
    }

    @Override
    protected void addAdditionalSaveData(@NonNull ValueOutput output) {
        ValueOutput child = output.child("Zenith");
        child.putDouble("Angle", this.getAngle());
        child.putInt("Age", this.getProgress());
        if (this.getOwnerUUID() != null) {
            child.store("Owner", UUIDUtil.CODEC, this.getOwnerUUID());
        }
        child.putInt("SwordType", this.getSwordType());
        child.store("Rotation", PosUtil.Rotation.CODEC, this.getIniRot());
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
            this.fixedPose[0] = Quaternion.trans(new Vector3(0, 0, 1), new Vector3(this.getReference()[2].getX(), 0, this.getReference()[2].getZ()));
            this.fixedPose[1] = Quaternion.trans(new Vector3(this.getReference()[2].getX(), 0, this.getReference()[2].getZ()), this.getReference()[2]);
            this.fixedPose[2] = Quaternion.rotate(this.getReference()[2], this.getAngle());
        }
        return this.fixedPose;
    }
}
