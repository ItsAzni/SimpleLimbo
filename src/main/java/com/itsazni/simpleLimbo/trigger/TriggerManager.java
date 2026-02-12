package com.itsazni.simpleLimbo.trigger;

import com.itsazni.simpleLimbo.SimpleLimbo;
import com.itsazni.simpleLimbo.config.AutoTriggerConfig;
import com.itsazni.simpleLimbo.util.MessageUtil;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class TriggerManager {

    private final SimpleLimbo plugin;
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private ScheduledTask afkTask;

    public TriggerManager(SimpleLimbo plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        AutoTriggerConfig.AfkTriggerConfig afk = plugin.getSettings().getAutoTriggers().getAfk();
        if (!afk.isEnabled()) {
            return;
        }

        long intervalSeconds = Math.max(1, afk.getCheckInterval());
        afkTask = plugin.getServer().getScheduler()
                .buildTask(plugin, this::checkAfkPlayers)
                .repeat(Duration.ofSeconds(intervalSeconds))
                .schedule();
    }

    public void stop() {
        if (afkTask != null) {
            afkTask.cancel();
            afkTask = null;
        }
    }

    public void markActivity(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void removePlayer(Player player) {
        lastActivity.remove(player.getUniqueId());
    }

    public boolean shouldFallback(String reason) {
        AutoTriggerConfig.FallbackTriggerConfig fallback = plugin.getSettings().getAutoTriggers().getFallback();
        if (!fallback.isEnabled()) {
            return false;
        }

        String value = reason == null ? "" : reason;
        for (String pattern : fallback.getKickPatterns()) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(value).matches()) {
                return true;
            }
        }
        return false;
    }

    private void checkAfkPlayers() {
        AutoTriggerConfig.AfkTriggerConfig afk = plugin.getSettings().getAutoTriggers().getAfk();
        if (!afk.isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        long idleMillis = Math.max(1, afk.getIdleTime()) * 1000L;

        plugin.getServer().getAllPlayers().forEach(player -> {
            if (plugin.getLimboManager().isPlayerInLimbo(player)) {
                return;
            }
            if (player.hasPermission(afk.getExemptPermission())) {
                return;
            }

            long last = lastActivity.getOrDefault(player.getUniqueId(), now);
            if (now - last >= idleMillis) {
                boolean sent = plugin.getLimboManager().sendPlayerToLimbo(player, afk.getLimbo());
                if (sent && !afk.getMessage().isEmpty()) {
                    player.sendMessage(MessageUtil.component(afk.getMessage()));
                }
                markActivity(player);
            }
        });
    }
}
