package com.gtceuterminal.client.blueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class OrbitalCamera {

    public static final float PITCH_MIN =  5f;   // degrees above horizon
    public static final float PITCH_MAX = 85f;   // nearly straight down
    public static final float DIST_MIN  =  3f;
    public static final float DIST_MAX  = 60f;
    private static final float YAW_DEFAULT   = 225f;  // south-west view
    private static final float PITCH_DEFAULT =  35f;
    private static final float DIST_DEFAULT  =  16f;

    public static final float DRAG_SENSITIVITY = 0.4f;
    public static final float SCROLL_SPEED     = 2.0f;

    private Vec3  target;
    private float yaw;
    private float pitch;
    private float dist;

    public OrbitalCamera(Vec3 target) {
        this.target = target;
        this.yaw    = YAW_DEFAULT;
        this.pitch  = PITCH_DEFAULT;
        this.dist   = DIST_DEFAULT;
    }

    public OrbitalCamera(BlockPos center) {
        this(Vec3.atCenterOf(center));
    }

    public void drag(double dx, double dy) {
        yaw   = (yaw + (float)(dx * DRAG_SENSITIVITY)) % 360f;
        pitch = Math.max(PITCH_MIN, Math.min(PITCH_MAX, pitch - (float)(dy * DRAG_SENSITIVITY)));
    }

    public void scroll(double delta) {
        dist = Math.max(DIST_MIN, Math.min(DIST_MAX, dist - (float)(delta * SCROLL_SPEED)));
    }

    // Compute
    public Vec3 getPosition() {
        double yawRad   = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        return new Vec3(
                target.x + dist * cosPitch * Math.sin(yawRad),
                target.y + dist * Math.sin(pitchRad),
                target.z + dist * cosPitch * Math.cos(yawRad));
    }

    public float getLookYaw() {
        return yaw + 180f;
    }


    public float getLookPitch() {
        return -pitch;
    }

    public Vec3  getTarget()  { return target; }

    public void  setTarget(Vec3 t) { this.target = t; }

    public float getYaw()     { return yaw; }

    public float getPitch()   { return pitch; }
    
    public float getDist()    { return dist; }
}