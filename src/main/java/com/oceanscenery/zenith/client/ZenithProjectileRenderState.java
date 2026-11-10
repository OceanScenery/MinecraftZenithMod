package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.util.Vector3;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class ZenithProjectileRenderState extends EntityRenderState {
    final ItemStackRenderState itemState = new ItemStackRenderState();
    final Vector3[] reference = new Vector3[3];
    final Quaternionf[] fixed = {new Quaternionf(), new Quaternionf(), new Quaternionf()};
    Vec3 eye;
    Vector3 normal;
    int swordType;
    double distance, angle, progressAngle, trailAngle, time, phase, pixelScale;
    boolean ready, lowerTrail;
}
