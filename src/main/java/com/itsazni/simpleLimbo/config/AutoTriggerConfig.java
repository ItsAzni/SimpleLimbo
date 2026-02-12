package com.itsazni.simpleLimbo.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class AutoTriggerConfig {

    @Comment("AFK auto-trigger settings")
    private AfkTriggerConfig afk = new AfkTriggerConfig();

    @Comment("Fallback trigger settings (when kicked from server)")
    private FallbackTriggerConfig fallback = new FallbackTriggerConfig();

    public AutoTriggerConfig() {}

    public AfkTriggerConfig getAfk() {
        return afk;
    }

    public FallbackTriggerConfig getFallback() {
        return fallback;
    }

    @ConfigSerializable
    public static class AfkTriggerConfig {

        @Comment("Enable AFK auto-trigger")
        private boolean enabled = false;

        @Comment("Target limbo name")
        private String limbo = "afk";

        @Comment("Seconds of idle time before considered AFK")
        private int idleTime = 300;

        @Comment("Check interval in seconds")
        private int checkInterval = 30;

        @Comment("Permission to exempt from AFK")
        private String exemptPermission = "simplelimbo.afk.exempt";

        @Comment("Message when moved to AFK")
        private String message = "&7You have been moved to AFK due to inactivity.";

        public AfkTriggerConfig() {}

        public boolean isEnabled() {
            return enabled;
        }

        public String getLimbo() {
            return limbo;
        }

        public int getIdleTime() {
            return idleTime;
        }

        public int getCheckInterval() {
            return checkInterval;
        }

        public String getExemptPermission() {
            return exemptPermission;
        }

        public String getMessage() {
            return message;
        }
    }

    @ConfigSerializable
    public static class FallbackTriggerConfig {

        @Comment("Enable fallback trigger")
        private boolean enabled = true;

        @Comment("Target limbo name")
        private String limbo = "fallback";

        @Comment("Kick message patterns that trigger fallback (regex)")
        private List<String> kickPatterns = new ArrayList<>(List.of(
                ".*server.*closed.*",
                ".*server.*restarting.*",
                ".*timed out.*",
                ".*kicked.*"
        ));

        @Comment("Message when moved to fallback")
        private String message = "&cServer is unavailable. You've been moved to fallback limbo.";

        public FallbackTriggerConfig() {}

        public boolean isEnabled() {
            return enabled;
        }

        public String getLimbo() {
            return limbo;
        }

        public List<String> getKickPatterns() {
            return kickPatterns;
        }

        public String getMessage() {
            return message;
        }
    }

}
