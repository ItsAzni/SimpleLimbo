package com.itsazni.simpleLimbo.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class LimboSettingsConfig {

    @Comment("Read timeout in milliseconds (must be > 0)")
    private long readTimeout = 30000;

    @Comment("Should player rejoin after disconnect?")
    private boolean shouldRejoin = true;

    @Comment("Should player respawn?")
    private boolean shouldRespawn = false;

    @Comment("Hide F3 debug info")
    private boolean reducedDebugInfo = true;

    @Comment("View distance (2-32)")
    private int viewDistance = 4;

    @Comment("Simulation distance (2-32)")
    private int simulationDistance = 4;

    @Comment("Disable player falling (keep floating)")
    private boolean disableFalling = true;

    @Comment("Delay before anti-fall activates (milliseconds)")
    private long disableFallingDelayMs = 5000;

    public LimboSettingsConfig() {}

    public long getReadTimeout() {
        return readTimeout;
    }

    public boolean isShouldRejoin() {
        return shouldRejoin;
    }

    public boolean isShouldRespawn() {
        return shouldRespawn;
    }

    public boolean isReducedDebugInfo() {
        return reducedDebugInfo;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public int getSimulationDistance() {
        return simulationDistance;
    }

    public boolean isDisableFalling() {
        return disableFalling;
    }

    public long getDisableFallingDelayMs() {
        return disableFallingDelayMs;
    }
}
