package com.itsazni.simpleLimbo.listener;

import com.itsazni.simpleLimbo.SimpleLimbo;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import com.itsazni.simpleLimbo.compat.ServerConnectionInjector;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.elytrium.limboapi.api.player.LimboPlayer;

import java.util.Optional;

public class PlayerListener {

    private final SimpleLimbo plugin;

    public PlayerListener(SimpleLimbo plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        plugin.getTriggerManager().markActivity(event.getPlayer());
    }

    /**
     * Handle initial server choice when player first joins the proxy.
     * This fires BEFORE any TCP connection attempt, so we can intercept alias servers early.
     */
    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        event.getInitialServer().ifPresent(server -> {
            String targetName = server.getServerInfo().getName();
            String limboId = plugin.getVelocityAliasBridge().resolveLimboByAlias(targetName);
            if (limboId == null || limboId.isBlank()) {
                return;
            }

            // Clear initial server to prevent Velocity from connecting
            event.setInitialServer(null);

            // Spawn player in limbo after a short delay (player needs to be fully connected)
            plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> {
                        if (event.getPlayer().isActive()) {
                            plugin.getLimboManager().sendPlayerToLimbo(event.getPlayer(), limboId);
                        }
                    })
                    .delay(50, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .schedule();

            if (plugin.getSettings().isDebug()) {
                plugin.getLogger().info("Intercepted initial server '{}' and will redirect {} to limbo '{}'",
                        targetName, event.getPlayer().getUsername(), limboId);
            }
        });
    }

    /**
     * Handle server transfer when player is already connected.
     * Uses FIRST priority to intercept before Velocity attempts TCP connection.
     */
    @Subscribe(order = PostOrder.FIRST)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        long startTime = System.currentTimeMillis();
        
        String targetName = event.getOriginalServer().getServerInfo().getName();
        String limboId = plugin.getVelocityAliasBridge().resolveLimboByAlias(targetName);
        if (limboId == null || limboId.isBlank()) {
            return;
        }

        if (plugin.getSettings().isDebug()) {
            plugin.getLogger().info("[TIMING] ServerPreConnectEvent fired for {} -> {} (resolved to limbo '{}')",
                    event.getPlayer().getUsername(), targetName, limboId);
        }

        boolean sent = plugin.getLimboManager().sendPlayerToLimbo(event.getPlayer(), limboId);
        if (!sent) {
            return;
        }
        event.setResult(ServerPreConnectEvent.ServerResult.denied());

        if (plugin.getSettings().isDebug()) {
            long elapsed = System.currentTimeMillis() - startTime;
            plugin.getLogger().info("[TIMING] Intercepted virtual alias '{}' and redirected {} to limbo '{}' in {}ms",
                    targetName, event.getPlayer().getUsername(), limboId, elapsed);
        }
    }

    /**
     * Intercept transfer from limbo to real server.
     * Uses LAST priority so this runs AFTER alias interception (which uses FIRST).
     * 
     * When a player in limbo tries to transfer to a real server (e.g., after auth),
     * we must use LimboPlayer.disconnect(server) instead of letting Velocity handle it.
     * This properly restores the session handler and avoids keepalive conflicts.
     */
    @Subscribe(order = PostOrder.LAST)
    public void onServerPreConnectClearFake(ServerPreConnectEvent event) {
        // Only handle if the transfer is actually happening (not denied by alias interception)
        if (!event.getResult().isAllowed()) {
            return;
        }

        // Check if player is in limbo
        if (!plugin.getLimboManager().isPlayerInLimbo(event.getPlayer())) {
            return;
        }

        // Get the LimboPlayer instance
        Optional<LimboPlayer> limboPlayerOpt = plugin.getLimboManager().getLimboPlayer(event.getPlayer());
        if (limboPlayerOpt.isEmpty()) {
            plugin.getLogger().warn("Player {} is tracked as in limbo but has no LimboPlayer instance",
                    event.getPlayer().getUsername());
            // Fall back to clearing fake server and letting Velocity handle it
            ServerConnectionInjector.clearFakeServer(event.getPlayer(), plugin.getLogger());
            return;
        }

        LimboPlayer limboPlayer = limboPlayerOpt.get();
        Optional<RegisteredServer> targetServerOpt = event.getResult().getServer();
        
        if (targetServerOpt.isEmpty()) {
            return;
        }

        RegisteredServer targetServer = targetServerOpt.get();

        // Deny the event - we'll handle the transfer ourselves
        event.setResult(ServerPreConnectEvent.ServerResult.denied());

        // Clear fake server connection first
        ServerConnectionInjector.clearFakeServer(event.getPlayer(), plugin.getLogger());

        // Use LimboPlayer.disconnect(server) to properly exit limbo and connect to backend
        // This restores the session handler correctly and avoids keepalive issues
        limboPlayer.disconnect(targetServer);

        if (plugin.getSettings().isDebug()) {
            plugin.getLogger().info("Intercepted transfer from limbo: {} -> {} (using limboPlayer.disconnect)",
                    event.getPlayer().getUsername(), targetServer.getServerInfo().getName());
        }
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        plugin.getTriggerManager().markActivity(event.getPlayer());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        plugin.getDisplayManager().clearDisplay(event.getPlayer());
        plugin.getTriggerManager().removePlayer(event.getPlayer());
        plugin.getLimboManager().onPlayerLeaveLimbo(event.getPlayer());
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        String reason = event.getServerKickReason()
                .map(PlainTextComponentSerializer.plainText()::serialize)
                .orElse("");

        if (!plugin.getTriggerManager().shouldFallback(reason)) {
            return;
        }

        String fallback = plugin.getSettings().getAutoTriggers().getFallback().getLimbo();
        boolean sent = plugin.getLimboManager().sendPlayerToLimbo(event.getPlayer(), fallback);
        if (sent) {
            event.setResult(KickedFromServerEvent.Notify.create(net.kyori.adventure.text.Component.empty()));
            String message = plugin.getSettings().getAutoTriggers().getFallback().getMessage();
            if (!message.isEmpty()) {
                event.getPlayer().sendMessage(com.itsazni.simpleLimbo.util.MessageUtil.component(message));
            }
        }
    }
}
