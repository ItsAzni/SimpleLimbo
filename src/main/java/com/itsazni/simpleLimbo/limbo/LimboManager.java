package com.itsazni.simpleLimbo.limbo;

import com.itsazni.simpleLimbo.SimpleLimbo;
import com.itsazni.simpleLimbo.config.LimboServerConfig;
import com.itsazni.simpleLimbo.config.Settings;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.player.LimboPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LimboManager {

    private final SimpleLimbo plugin;
    private final LimboFactory factory;
    private final Map<String, LimboInstance> limbos = new ConcurrentHashMap<>();
    
    // Track which players are in which limbo
    private final Map<UUID, String> playerLimbos = new ConcurrentHashMap<>();
    
    // Track LimboPlayer instances for proper disconnect handling
    private final Map<UUID, LimboPlayer> limboPlayers = new ConcurrentHashMap<>();

    public LimboManager(SimpleLimbo plugin, LimboFactory factory) {
        this.plugin = plugin;
        this.factory = factory;
    }

    public void loadAll() {
        Settings settings = plugin.getSettings();
        
        // Clear existing limbos
        limbos.clear();

        // Load each limbo from config
        for (Map.Entry<String, LimboServerConfig> entry : settings.getLimbos().entrySet()) {
            String name = entry.getKey();
            LimboServerConfig config = entry.getValue();

            if (!config.isEnabled()) {
                plugin.getLogger().info("Skipping disabled limbo: {}", name);
                continue;
            }

            try {
                LimboInstance instance = new LimboInstance(plugin, name, config, factory);
                instance.create();
                limbos.put(name, instance);
            } catch (Exception e) {
                plugin.getLogger().error("Failed to create limbo: {}", name, e);
            }
        }

        plugin.getLogger().info("Loaded {} limbo servers", limbos.size());
    }

    public void reload() {
        plugin.getLogger().info("Reloading limbo servers...");
        
        // Clear player tracking (players in old limbos will be disconnected)
        playerLimbos.clear();
        
        // Reload all limbos
        loadAll();
    }

    public Optional<LimboInstance> getLimbo(String name) {
        return Optional.ofNullable(limbos.get(name));
    }

    public Set<String> getLimboNames() {
        return limbos.keySet();
    }

    public Map<String, LimboInstance> getAllLimbos() {
        return limbos;
    }

    public boolean sendPlayerToLimbo(Player player, String limboName) {
        LimboInstance limbo = limbos.get(limboName);
        if (limbo == null) {
            plugin.getLogger().warn("Cannot send player {} to limbo {}: limbo not found", 
                    player.getUsername(), limboName);
            return false;
        }

        limbo.spawnPlayer(player);
        playerLimbos.put(player.getUniqueId(), limboName);
        
        if (plugin.getSettings().isDebug()) {
            plugin.getLogger().info("Sent player {} to limbo {}", player.getUsername(), limboName);
        }
        
        return true;
    }

    public void onPlayerLeaveLimbo(Player player) {
        playerLimbos.remove(player.getUniqueId());
        limboPlayers.remove(player.getUniqueId());
    }

    /**
     * Register a LimboPlayer instance for a player.
     * This allows us to properly disconnect players from limbo.
     */
    public void registerLimboPlayer(Player player, LimboPlayer limboPlayer) {
        limboPlayers.put(player.getUniqueId(), limboPlayer);
    }

    /**
     * Get the LimboPlayer instance for a player, if they're in limbo.
     */
    public Optional<LimboPlayer> getLimboPlayer(Player player) {
        return Optional.ofNullable(limboPlayers.get(player.getUniqueId()));
    }

    public Optional<String> getPlayerLimbo(Player player) {
        return Optional.ofNullable(playerLimbos.get(player.getUniqueId()));
    }

    public boolean isPlayerInLimbo(Player player) {
        return playerLimbos.containsKey(player.getUniqueId());
    }

    public int getPlayerCount(String limboName) {
        return (int) playerLimbos.values().stream()
                .filter(name -> name.equals(limboName))
                .count();
    }

    public int getTotalPlayersInLimbos() {
        return playerLimbos.size();
    }

    public LimboFactory getFactory() {
        return factory;
    }
}
