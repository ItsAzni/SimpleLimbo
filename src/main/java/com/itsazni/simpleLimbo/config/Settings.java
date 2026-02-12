package com.itsazni.simpleLimbo.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.HashMap;
import java.util.Map;

@ConfigSerializable
public class Settings {

    @Comment("Enable debug logging")
    private boolean debug = false;

    @Comment("Auto-trigger settings")
    private AutoTriggerConfig autoTriggers = new AutoTriggerConfig();

    @Comment("Limbo server configurations")
    private Map<String, LimboServerConfig> limbos = new HashMap<>();

    @Comment("Bridge aliases for plugins that require Velocity [servers] names")
    private VelocityBridgeConfig velocityBridge = new VelocityBridgeConfig();

    public Settings() {
        // Create default limbo configurations
        createDefaultLimbos();
    }

    private void createDefaultLimbos() {
        // AFK Limbo
        LimboServerConfig afk = new LimboServerConfig();
        limbos.put("afk", afk);

        // Fallback Limbo
        LimboServerConfig fallback = new LimboServerConfig();
        limbos.put("fallback", fallback);
    }

    public boolean isDebug() {
        return debug;
    }

    public AutoTriggerConfig getAutoTriggers() {
        return autoTriggers;
    }

    public Map<String, LimboServerConfig> getLimbos() {
        return limbos;
    }

    public LimboServerConfig getLimbo(String name) {
        return limbos.get(name);
    }

    public VelocityBridgeConfig getVelocityBridge() {
        return velocityBridge;
    }
}
