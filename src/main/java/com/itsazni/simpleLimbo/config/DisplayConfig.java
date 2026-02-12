package com.itsazni.simpleLimbo.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class DisplayConfig {

    @Comment("Settings when player joins limbo")
    private OnJoinConfig onJoin = new OnJoinConfig();

    @Comment("BossBar settings while in limbo")
    private BossBarConfig bossbar = new BossBarConfig();

    @Comment("Periodic actionbar settings")
    private PeriodicActionBarConfig actionbar = new PeriodicActionBarConfig();

    public DisplayConfig() {}

    public OnJoinConfig getOnJoin() {
        return onJoin;
    }

    public BossBarConfig getBossbar() {
        return bossbar;
    }

    public PeriodicActionBarConfig getActionbar() {
        return actionbar;
    }

    @ConfigSerializable
    public static class OnJoinConfig {

        @Comment("Chat message on join (supports color codes with &)")
        private String chat = "&7Welcome to limbo!";

        @Comment("Title settings")
        private TitleConfig title = new TitleConfig();

        @Comment("ActionBar on join")
        private ActionBarOnJoinConfig actionbar = new ActionBarOnJoinConfig();

        public OnJoinConfig() {}

        public String getChat() {
            return chat;
        }

        public TitleConfig getTitle() {
            return title;
        }

        public ActionBarOnJoinConfig getActionbar() {
            return actionbar;
        }
    }

    @ConfigSerializable
    public static class TitleConfig {

        @Comment("Enable title on join")
        private boolean enabled = true;

        @Comment("Title text")
        private String title = "&7Limbo";

        @Comment("Subtitle text")
        private String subtitle = "&8Type /leave to exit";

        @Comment("Fade in time (ticks)")
        private int fadeIn = 10;

        @Comment("Stay time (ticks)")
        private int stay = 70;

        @Comment("Fade out time (ticks)")
        private int fadeOut = 20;

        public TitleConfig() {}

        public boolean isEnabled() {
            return enabled;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public int getFadeIn() {
            return fadeIn;
        }

        public int getStay() {
            return stay;
        }

        public int getFadeOut() {
            return fadeOut;
        }
    }

    @ConfigSerializable
    public static class ActionBarOnJoinConfig {

        @Comment("Enable actionbar on join")
        private boolean enabled = false;

        @Comment("ActionBar message")
        private String message = "";

        public ActionBarOnJoinConfig() {}

        public boolean isEnabled() {
            return enabled;
        }

        public String getMessage() {
            return message;
        }
    }

    @ConfigSerializable
    public static class BossBarConfig {

        @Comment("Enable bossbar while in limbo")
        private boolean enabled = true;

        @Comment("BossBar title")
        private String title = "&7Limbo";

        @Comment("BossBar color: PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE")
        private String color = "WHITE";

        @Comment("BossBar style: SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20")
        private String style = "SOLID";

        @Comment("BossBar progress (0.0 - 1.0)")
        private float progress = 1.0f;

        public BossBarConfig() {}

        public boolean isEnabled() {
            return enabled;
        }

        public String getTitle() {
            return title;
        }

        public String getColor() {
            return color;
        }

        public String getStyle() {
            return style;
        }

        public float getProgress() {
            return progress;
        }
    }

    @ConfigSerializable
    public static class PeriodicActionBarConfig {

        @Comment("Enable periodic actionbar")
        private boolean enabled = false;

        @Comment("ActionBar message (supports {countdown} placeholder)")
        private String message = "";

        @Comment("Interval in ticks between updates")
        private int interval = 40;

        public PeriodicActionBarConfig() {}

        public boolean isEnabled() {
            return enabled;
        }

        public String getMessage() {
            return message;
        }

        public int getInterval() {
            return interval;
        }
    }
}
