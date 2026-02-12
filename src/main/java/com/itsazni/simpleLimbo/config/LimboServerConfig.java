package com.itsazni.simpleLimbo.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class LimboServerConfig {

    @Comment("Enable this limbo server")
    private boolean enabled = true;

    @Comment("Dimension: OVERWORLD, NETHER, THE_END")
    private String dimension = "OVERWORLD";

    @Comment("Game mode: SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR")
    private String gamemode = "ADVENTURE";

    @Comment("World time (0-24000, 6000 = noon, 18000 = midnight)")
    private long worldTime = 6000;

    @Comment("Spawn location")
    private SpawnConfig spawn = new SpawnConfig();

    @Comment("Limbo settings")
    private LimboSettingsConfig settings = new LimboSettingsConfig();

    @Comment("World file configuration (optional schematic loading)")
    private WorldFileConfig worldFile = new WorldFileConfig();

    @Comment("Commands allowed in this limbo (also used for chat suggestions)")
    private List<String> commands = new ArrayList<>();

    @Comment("Display settings (BossBar, Title, ActionBar)")
    private DisplayConfig display = new DisplayConfig();

    @Comment("Auto-reconnect settings (for fallback limbos)")
    private AutoReconnectConfig autoReconnect = new AutoReconnectConfig();

    @Comment("Fake server name for auth plugin compatibility. When set, this limbo will appear " +
             "to auth plugins as if the player is connected to this backend server. " +
             "Leave empty to disable. Example: 'auth' or 'limbo-auth'")
    private String fakeServerName = "";

    public LimboServerConfig() {}

    public boolean isEnabled() {
        return enabled;
    }

    public String getDimension() {
        return dimension;
    }

    public String getGamemode() {
        return gamemode;
    }

    public long getWorldTime() {
        return worldTime;
    }

    public SpawnConfig getSpawn() {
        return spawn;
    }

    public LimboSettingsConfig getSettings() {
        return settings;
    }

    public WorldFileConfig getWorldFile() {
        return worldFile;
    }

    public List<String> getCommands() {
        return commands;
    }

    public DisplayConfig getDisplay() {
        return display;
    }

    public AutoReconnectConfig getAutoReconnect() {
        return autoReconnect;
    }

    public String getFakeServerName() {
        return fakeServerName;
    }

    @ConfigSerializable
    public static class AutoReconnectConfig {

        @Comment("Enable auto-reconnect to server")
        private boolean enabled = false;

        @Comment("Interval in seconds between reconnect attempts")
        private int interval = 30;

        @Comment("Target server to reconnect to")
        private String server = "lobby";

        @Comment("Message when attempting reconnect")
        private String message = "&7Attempting to reconnect...";

        @Comment("Message when reconnect successful")
        private String successMessage = "&aReconnected successfully!";

        public AutoReconnectConfig() {}

        public boolean isEnabled() {
            return enabled;
        }

        public int getInterval() {
            return interval;
        }

        public String getServer() {
            return server;
        }

        public String getMessage() {
            return message;
        }

        public String getSuccessMessage() {
            return successMessage;
        }
    }
}
