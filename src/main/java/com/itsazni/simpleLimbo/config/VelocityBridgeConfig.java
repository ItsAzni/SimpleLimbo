package com.itsazni.simpleLimbo.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigSerializable
public class VelocityBridgeConfig {

    @Comment("Enable bridge for plugins that require server name from [servers]")
    private boolean enabled = true;

    @Comment("Register virtual aliases into Velocity server registry")
    private boolean registerAliases = true;

    @Comment("Replace existing Velocity server with same alias")
    private boolean overrideExisting = true;

    @Comment("Bind host for registered dummy aliases")
    private String host = "127.0.0.1";

    @Comment("Starting port for dummy aliases (use port 1 for instant connection refused, avoiding TCP timeout)")
    private int startPort = 1;

    @Comment("Map: velocity server alias -> limbo id")
    private Map<String, String> aliases = new LinkedHashMap<>(Map.of("auth", "auth"));

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRegisterAliases() {
        return registerAliases;
    }

    public boolean isOverrideExisting() {
        return overrideExisting;
    }

    public String getHost() {
        return host;
    }

    public int getStartPort() {
        return startPort;
    }

    public Map<String, String> getAliases() {
        return aliases;
    }
}
