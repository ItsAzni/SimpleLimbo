package com.itsazni.simpleLimbo.display;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;

public class LimboDisplay {

    private final Player player;
    private final BossBar bossBar;
    private final ScheduledTask actionBarTask;

    public LimboDisplay(Player player, BossBar bossBar, ScheduledTask actionBarTask) {
        this.player = player;
        this.bossBar = bossBar;
        this.actionBarTask = actionBarTask;
    }

    public void clear() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }
}
