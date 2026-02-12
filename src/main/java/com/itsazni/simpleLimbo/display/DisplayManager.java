package com.itsazni.simpleLimbo.display;

import com.itsazni.simpleLimbo.SimpleLimbo;
import com.itsazni.simpleLimbo.config.DisplayConfig;
import com.itsazni.simpleLimbo.config.LimboServerConfig;
import com.itsazni.simpleLimbo.util.MessageUtil;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisplayManager {

    private final SimpleLimbo plugin;
    private final Map<UUID, LimboDisplay> activeDisplays = new ConcurrentHashMap<>();

    public DisplayManager(SimpleLimbo plugin) {
        this.plugin = plugin;
    }

    public void showJoinDisplay(Player player, LimboServerConfig config) {
        DisplayConfig display = config.getDisplay();
        DisplayConfig.OnJoinConfig onJoin = display.getOnJoin();

        if (!onJoin.getChat().isEmpty()) {
            player.sendMessage(MessageUtil.component(onJoin.getChat()));
        }

        DisplayConfig.TitleConfig titleConfig = onJoin.getTitle();
        if (titleConfig.isEnabled()) {
            Title.Times times = Title.Times.times(
                    Duration.ofMillis(titleConfig.getFadeIn() * 50L),
                    Duration.ofMillis(titleConfig.getStay() * 50L),
                    Duration.ofMillis(titleConfig.getFadeOut() * 50L)
            );
            player.showTitle(Title.title(
                    MessageUtil.component(titleConfig.getTitle()),
                    MessageUtil.component(titleConfig.getSubtitle()),
                    times
            ));
        }

        DisplayConfig.ActionBarOnJoinConfig onJoinActionBar = onJoin.getActionbar();
        if (onJoinActionBar.isEnabled() && !onJoinActionBar.getMessage().isEmpty()) {
            player.sendActionBar(MessageUtil.component(onJoinActionBar.getMessage()));
        }

        BossBar bossBar = null;
        DisplayConfig.BossBarConfig bossBarConfig = display.getBossbar();
        if (bossBarConfig.isEnabled()) {
            bossBar = BossBar.bossBar(
                    MessageUtil.component(bossBarConfig.getTitle()),
                    normalizeProgress(bossBarConfig.getProgress()),
                    parseColor(bossBarConfig.getColor()),
                    parseOverlay(bossBarConfig.getStyle())
            );
            player.showBossBar(bossBar);
        }

        ScheduledTask actionBarTask = null;
        DisplayConfig.PeriodicActionBarConfig periodicActionbar = display.getActionbar();
        if (periodicActionbar.isEnabled() && !periodicActionbar.getMessage().isEmpty()) {
            long intervalMs = Math.max(1, periodicActionbar.getInterval()) * 50L;
            actionBarTask = plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> player.sendActionBar(MessageUtil.component(periodicActionbar.getMessage())))
                    .repeat(Duration.ofMillis(intervalMs))
                    .schedule();
        }

        activeDisplays.put(player.getUniqueId(), new LimboDisplay(player, bossBar, actionBarTask));
    }

    public void clearDisplay(Player player) {
        LimboDisplay display = activeDisplays.remove(player.getUniqueId());
        if (display != null) {
            display.clear();
        }
    }

    public void clearAll() {
        activeDisplays.values().forEach(LimboDisplay::clear);
        activeDisplays.clear();
    }

    public Component formatActionBar(String template, int countdownSeconds) {
        String message = MessageUtil.replace(template, "{countdown}", String.valueOf(countdownSeconds));
        return MessageUtil.component(message);
    }

    private float normalizeProgress(float progress) {
        if (progress < 0f) {
            return 0f;
        }
        return Math.min(progress, 1f);
    }

    private BossBar.Color parseColor(String color) {
        try {
            return BossBar.Color.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.WHITE;
        }
    }

    private BossBar.Overlay parseOverlay(String overlay) {
        try {
            return BossBar.Overlay.valueOf(overlay.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}
