package com.itsazni.simpleLimbo.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class SpawnConfig {

    @Comment("X coordinate")
    private double x = 0.0;

    @Comment("Y coordinate")
    private double y = 100.0;

    @Comment("Z coordinate")
    private double z = 0.0;

    @Comment("Yaw (rotation)")
    private float yaw = 0.0f;

    @Comment("Pitch (head tilt)")
    private float pitch = 0.0f;

    public SpawnConfig() {}

    public SpawnConfig(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
