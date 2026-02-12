package com.itsazni.simpleLimbo.limbo.handler;

import com.itsazni.simpleLimbo.SimpleLimbo;
import com.itsazni.simpleLimbo.config.LimboServerConfig;
import com.itsazni.simpleLimbo.compat.ServerConnectionInjector;
import com.itsazni.simpleLimbo.limbo.LimboInstance;
import com.itsazni.simpleLimbo.util.MessageUtil;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.elytrium.limboapi.api.Limbo;

import java.util.List;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SimpleLimboHandler implements LimboSessionHandler {

    private final SimpleLimbo plugin;
    private final LimboInstance instance;
    private final Player proxyPlayer;

    private LimboPlayer limboPlayer;
    private ScheduledFuture<?> autoReconnectTask;
    private ScheduledFuture<?> disableFallingFallbackTask;
    private int countdownSeconds;
    private boolean disableFallingRequested;
    private boolean disableFallingApplied;
    private long disableFallingDelayMs;
    private long spawnMillis;
    private Double holdY;
    private float lastYaw;
    private float lastPitch;

    public SimpleLimboHandler(SimpleLimbo plugin, LimboInstance instance, Player proxyPlayer) {
        this.plugin = plugin;
        this.instance = instance;
        this.proxyPlayer = proxyPlayer;
    }

    @Override
    public void onSpawn(Limbo server, LimboPlayer player) {
        this.limboPlayer = player;

        LimboServerConfig config = instance.getConfig();
        this.spawnMillis = System.currentTimeMillis();
        this.lastYaw = config.getSpawn().getYaw();
        this.lastPitch = config.getSpawn().getPitch();

        if (config.getSettings().isDisableFalling()) {
            this.disableFallingRequested = true;
            this.disableFallingApplied = false;
            this.disableFallingDelayMs = Math.max(0, config.getSettings().getDisableFallingDelayMs());
            this.holdY = null;
            this.disableFallingFallbackTask = player.getScheduledExecutor().schedule(
                    this::applyDisableFalling,
                    this.disableFallingDelayMs + 8000,
                    TimeUnit.MILLISECONDS
            );
        } else {
            this.disableFallingRequested = false;
            this.disableFallingApplied = false;
            this.disableFallingDelayMs = 0;
            this.holdY = null;
            player.enableFalling();
        }

        if (!config.getCommands().isEmpty()) {
            proxyPlayer.addCustomChatCompletions(config.getCommands());
        }

        plugin.getDisplayManager().showJoinDisplay(proxyPlayer, config);
        plugin.getTriggerManager().markActivity(proxyPlayer);

        // Register LimboPlayer for proper disconnect handling
        plugin.getLimboManager().registerLimboPlayer(proxyPlayer, player);

        // Inject fake server connection for auth plugin compatibility
        injectFakeServerIfConfigured(config);

        startAutoReconnectIfEnabled(config);
    }

    @Override
    public void onConfig(Limbo server, LimboPlayer player) {
        this.limboPlayer = player;
    }

    @Override
    public void onGeneric(Object packet) {
        applyDisableFalling();
    }

    @Override
    public void onMove(double posX, double posY, double posZ) {
        enforceAntiFall(posX, posY, posZ, lastYaw, lastPitch);
    }

    @Override
    public void onMove(double posX, double posY, double posZ, float yaw, float pitch) {
        this.lastYaw = yaw;
        this.lastPitch = pitch;
        enforceAntiFall(posX, posY, posZ, yaw, pitch);
    }

    @Override
    public void onChat(String chat) {
        plugin.getTriggerManager().markActivity(proxyPlayer);

        if (chat == null || chat.isBlank()) {
            return;
        }

        if (!chat.startsWith("/")) {
            return;
        }

        String commandLine = chat.substring(1).trim();
        if (commandLine.isEmpty()) {
            return;
        }

        String commandRoot = commandLine.split("\\s+")[0].toLowerCase();
        
        // Commands whitelist - only allow commands that are in the config list
        List<String> allowedCommands = instance.getConfig().getCommands();
        if (allowedCommands.isEmpty()) {
            // No commands configured = no commands allowed
            proxyPlayer.sendMessage(MessageUtil.component("&cCommands are disabled in this limbo."));
            return;
        }
        
        boolean isAllowed = allowedCommands.stream()
                .anyMatch(cmd -> cmd.toLowerCase().equals(commandRoot));
        
        if (!isAllowed) {
            proxyPlayer.sendMessage(MessageUtil.component("&cThis command is not available here."));
            return;
        }

        // Execute command through Velocity command manager
        plugin.getServer().getCommandManager().executeAsync(proxyPlayer, commandLine)
                .whenComplete((executed, throwable) -> {
                    if (throwable != null) {
                        // Check if this is the JPremium NPE issue
                        if (throwable instanceof NullPointerException || 
                            (throwable.getCause() != null && throwable.getCause() instanceof NullPointerException)) {
                            plugin.getLogger().warn("Command '{}' failed for {} - likely auth plugin incompatibility with limbo (no backend server connection)",
                                    commandLine, proxyPlayer.getUsername());
                            proxyPlayer.sendMessage(MessageUtil.component(
                                    "&cThis command requires a backend server connection. " +
                                    "&7Auth plugins like JPremium may not work in limbo. " +
                                    "Consider using LimboAuth instead."));
                        } else {
                            plugin.getLogger().error("Failed to execute command '{}' from limbo {} for {}",
                                    commandLine, instance.getName(), proxyPlayer.getUsername(), throwable);
                            proxyPlayer.sendMessage(MessageUtil.component("&cCommand failed: " + throwable.getMessage()));
                        }
                    }
                });
    }

    @Override
    public void onDisconnect() {
        if (autoReconnectTask != null) {
            autoReconnectTask.cancel(true);
        }
        if (disableFallingFallbackTask != null) {
            disableFallingFallbackTask.cancel(true);
        }
        if (!instance.getConfig().getCommands().isEmpty()) {
            proxyPlayer.removeCustomChatCompletions(instance.getConfig().getCommands());
        }
        
        // Clear fake server connection if it was injected
        ServerConnectionInjector.clearFakeServer(proxyPlayer, plugin.getLogger());
        
        plugin.getDisplayManager().clearDisplay(proxyPlayer);
        plugin.getLimboManager().onPlayerLeaveLimbo(proxyPlayer);
    }

    private void startAutoReconnectIfEnabled(LimboServerConfig config) {
        LimboServerConfig.AutoReconnectConfig reconnect = config.getAutoReconnect();
        if (!reconnect.isEnabled() || limboPlayer == null) {
            return;
        }

        int interval = Math.max(1, reconnect.getInterval());
        this.countdownSeconds = interval;

        autoReconnectTask = limboPlayer.getScheduledExecutor().scheduleAtFixedRate(() -> {
            if (countdownSeconds <= 0) {
                Optional<RegisteredServer> target = plugin.getServer().getServer(reconnect.getServer());
                if (target.isPresent()) {
                    proxyPlayer.createConnectionRequest(target.get()).connect().whenComplete((result, throwable) -> {
                        if (throwable == null && result != null && result.isSuccessful()) {
                            proxyPlayer.sendMessage(MessageUtil.component(reconnect.getSuccessMessage()));
                            if (limboPlayer != null) {
                                limboPlayer.disconnect(target.get());
                            }
                        }
                    });
                }
                countdownSeconds = interval;
                return;
            }

            String actionbar = MessageUtil.replace(config.getDisplay().getActionbar().getMessage(), "{countdown}", String.valueOf(countdownSeconds));
            if (!actionbar.isEmpty()) {
                proxyPlayer.sendActionBar(MessageUtil.component(actionbar));
            }
            countdownSeconds--;
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void applyDisableFalling() {
        if (!disableFallingRequested || disableFallingApplied || limboPlayer == null) {
            return;
        }

        long sinceSpawn = System.currentTimeMillis() - spawnMillis;
        if (sinceSpawn < disableFallingDelayMs) {
            return;
        }

        disableFallingApplied = true;
        LimboServerConfig limboConfig = instance.getConfig();
        holdY = limboConfig.getSpawn().getY();
        limboPlayer.teleport(
                limboConfig.getSpawn().getX(),
                limboConfig.getSpawn().getY(),
                limboConfig.getSpawn().getZ(),
                limboConfig.getSpawn().getYaw(),
                limboConfig.getSpawn().getPitch()
        );
        lastYaw = limboConfig.getSpawn().getYaw();
        lastPitch = limboConfig.getSpawn().getPitch();

        if (plugin.getSettings().isDebug()) {
            plugin.getLogger().info("Activated anti-fall for {} in '{}' after {}ms", proxyPlayer.getUsername(), instance.getName(), sinceSpawn);
        }

        if (disableFallingFallbackTask != null) {
            disableFallingFallbackTask.cancel(false);
            disableFallingFallbackTask = null;
        }
    }

    private void enforceAntiFall(double posX, double posY, double posZ, float yaw, float pitch) {
        applyDisableFalling();
        if (!disableFallingApplied || limboPlayer == null) {
            return;
        }

        if (holdY == null) {
            holdY = posY;
            return;
        }

        if (posY < holdY - 0.02) {
            limboPlayer.teleport(posX, holdY, posZ, yaw, pitch);
        }
    }

    /**
     * Inject a fake server connection if configured.
     * This allows auth plugins like JPremium to work in limbo by making
     * player.getCurrentServer() return a valid (fake) server connection.
     */
    private void injectFakeServerIfConfigured(LimboServerConfig config) {
        String fakeServerName = config.getFakeServerName();
        if (fakeServerName == null || fakeServerName.isBlank()) {
            return;
        }

        if (!ServerConnectionInjector.isAvailable()) {
            plugin.getLogger().warn("Fake server injection not available - auth plugins may not work in limbo '{}'", 
                    instance.getName());
            return;
        }

        Optional<RegisteredServer> fakeServer = plugin.getServer().getServer(fakeServerName);
        if (fakeServer.isEmpty()) {
            plugin.getLogger().warn("Fake server '{}' not found in Velocity - cannot inject for limbo '{}'",
                    fakeServerName, instance.getName());
            return;
        }

        boolean injected = ServerConnectionInjector.injectFakeServer(
                proxyPlayer, fakeServer.get(), plugin.getServer(), plugin.getLogger());
        if (injected) {
            plugin.getLogger().debug("Injected fake server '{}' for player {} in limbo '{}'",
                    fakeServerName, proxyPlayer.getUsername(), instance.getName());
        }
    }

}
