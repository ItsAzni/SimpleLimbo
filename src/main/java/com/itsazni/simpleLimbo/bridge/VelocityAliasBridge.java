package com.itsazni.simpleLimbo.bridge;

import com.itsazni.simpleLimbo.SimpleLimbo;
import com.itsazni.simpleLimbo.config.VelocityBridgeConfig;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityAliasBridge {

    private final SimpleLimbo plugin;
    private final Set<String> registeredAliases = ConcurrentHashMap.newKeySet();

    public VelocityAliasBridge(SimpleLimbo plugin) {
        this.plugin = plugin;
    }

    public void registerAliases() {
        VelocityBridgeConfig cfg = plugin.getSettings().getVelocityBridge();
        if (!cfg.isEnabled() || !cfg.isRegisterAliases()) {
            return;
        }

        int port = Math.max(1, cfg.getStartPort());
        for (Map.Entry<String, String> entry : cfg.getAliases().entrySet()) {
            String alias = entry.getKey();
            if (alias == null || alias.isBlank()) {
                continue;
            }

            plugin.getServer().getServer(alias).ifPresent(existing -> {
                if (cfg.isOverrideExisting()) {
                    plugin.getServer().unregisterServer(existing.getServerInfo());
                    plugin.getLogger().warn("Overriding existing Velocity server alias '{}' for limbo bridge", alias);
                }
            });

            if (plugin.getServer().getServer(alias).isPresent()) {
                plugin.getLogger().warn("Skipping alias '{}' because it already exists and overrideExisting=false", alias);
                continue;
            }

            ServerInfo info = new ServerInfo(alias, new InetSocketAddress(cfg.getHost(), port++));
            plugin.getServer().registerServer(info);
            registeredAliases.add(alias.toLowerCase());
            plugin.getLogger().info("Registered virtual alias '{}' -> limbo '{}'", alias, entry.getValue());
        }
    }

    public void unregisterAliases() {
        for (String alias : registeredAliases) {
            plugin.getServer().getServer(alias).ifPresent(server -> plugin.getServer().unregisterServer(server.getServerInfo()));
        }
        registeredAliases.clear();
    }

    public String resolveLimboByAlias(String alias) {
        VelocityBridgeConfig cfg = plugin.getSettings().getVelocityBridge();
        if (!cfg.isEnabled() || alias == null) {
            return null;
        }

        for (Map.Entry<String, String> entry : cfg.getAliases().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(alias)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public boolean isManagedAlias(String name) {
        if (name == null) {
            return false;
        }
        return registeredAliases.contains(name.toLowerCase());
    }

    public Optional<String> findFirstRealServerName() {
        return plugin.getServer().getAllServers().stream()
                .map(server -> server.getServerInfo().getName())
                .filter(name -> !isManagedAlias(name))
                .findFirst();
    }
}
