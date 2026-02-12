package com.itsazni.simpleLimbo.limbo;

import com.itsazni.simpleLimbo.SimpleLimbo;
import com.itsazni.simpleLimbo.config.LimboServerConfig;
import com.itsazni.simpleLimbo.config.WorldFileConfig;
import com.itsazni.simpleLimbo.limbo.handler.SimpleLimboHandler;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.command.LimboCommandMeta;
import net.elytrium.limboapi.api.file.BuiltInWorldFileType;
import net.elytrium.limboapi.api.file.WorldFile;
import net.elytrium.limboapi.api.player.GameMode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class LimboInstance {

    private final SimpleLimbo plugin;
    private final String name;
    private final LimboServerConfig config;
    private final LimboFactory factory;
    
    private Limbo limbo;
    private VirtualWorld world;

    public LimboInstance(SimpleLimbo plugin, String name, LimboServerConfig config, LimboFactory factory) {
        this.plugin = plugin;
        this.name = name;
        this.config = config;
        this.factory = factory;
    }

    public void create() {
        // Parse dimension
        Dimension dimension = parseDimension(config.getDimension());
        
        // Create virtual world
        this.world = factory.createVirtualWorld(
                dimension,
                config.getSpawn().getX(),
                config.getSpawn().getY(),
                config.getSpawn().getZ(),
                config.getSpawn().getYaw(),
                config.getSpawn().getPitch()
        );

        // Load world file if configured
        if (config.getWorldFile().isEnabled()) {
            loadWorldFile();
        }

        // Parse gamemode
        GameMode gameMode = parseGameMode(config.getGamemode());
        int readTimeout = sanitizeReadTimeout(config.getSettings().getReadTimeout());

        // Create limbo
        this.limbo = factory.createLimbo(world)
                .setName(name)
                .setWorldTime(config.getWorldTime())
                .setGameMode(gameMode)
                .setReadTimeout(readTimeout)
                .setShouldRejoin(config.getSettings().isShouldRejoin())
                .setShouldRespawn(config.getSettings().isShouldRespawn())
                .setReducedDebugInfo(config.getSettings().isReducedDebugInfo())
                .setViewDistance(config.getSettings().getViewDistance())
                .setSimulationDistance(config.getSettings().getSimulationDistance());

        // Register commands
        for (String command : config.getCommands()) {
            this.limbo.registerCommand(new LimboCommandMeta(List.of(command)));
        }

        plugin.getLogger().info("Created limbo server: {}", name);
    }

    private void loadWorldFile() {
        WorldFileConfig worldFileConfig = config.getWorldFile();
        Path worldPath = plugin.getDataDirectory().resolve(worldFileConfig.getPath());

        if (!worldPath.toFile().exists()) {
            plugin.getLogger().warn("World file not found for limbo {}: {}", name, worldPath);
            return;
        }

        try {
            BuiltInWorldFileType fileType = parseWorldFileType(worldFileConfig.getType());
            WorldFile worldFile = factory.openWorldFile(fileType, worldPath);

            worldFile.toWorld(
                    factory,
                    world,
                    worldFileConfig.getOffset().getX(),
                    worldFileConfig.getOffset().getY(),
                    worldFileConfig.getOffset().getZ(),
                    worldFileConfig.getLightLevel()
            );

            plugin.getLogger().info("Loaded world file for limbo {}: {}", name, worldPath);

        } catch (IOException e) {
            plugin.getLogger().error("Failed to load world file for limbo {}", name, e);
        }
    }

    public void spawnPlayer(Player player) {
        if (limbo == null) {
            plugin.getLogger().error("Cannot spawn player in limbo {}: limbo not created", name);
            return;
        }

        boolean debug = plugin.getSettings().isDebug();
        long startTime = debug ? System.currentTimeMillis() : 0;
        
        SimpleLimboHandler handler = new SimpleLimboHandler(plugin, this, player);
        limbo.spawnPlayer(player, handler);
        
        if (debug) {
            plugin.getLogger().info("limbo.spawnPlayer() for {} in limbo '{}' took {}ms", 
                    player.getUsername(), name, System.currentTimeMillis() - startTime);
        }
    }

    private Dimension parseDimension(String dimensionStr) {
        return switch (dimensionStr.toUpperCase()) {
            case "NETHER", "THE_NETHER" -> Dimension.NETHER;
            case "END", "THE_END" -> Dimension.THE_END;
            default -> Dimension.OVERWORLD;
        };
    }

    private GameMode parseGameMode(String gameModeStr) {
        return switch (gameModeStr.toUpperCase()) {
            case "SURVIVAL" -> GameMode.SURVIVAL;
            case "CREATIVE" -> GameMode.CREATIVE;
            case "SPECTATOR" -> GameMode.SPECTATOR;
            default -> GameMode.ADVENTURE;
        };
    }

    private BuiltInWorldFileType parseWorldFileType(String typeStr) {
        return switch (typeStr.toUpperCase()) {
            case "WORLDEDIT_SCHEM", "SCHEM" -> BuiltInWorldFileType.WORLDEDIT_SCHEM;
            case "STRUCTURE" -> BuiltInWorldFileType.STRUCTURE;
            default -> BuiltInWorldFileType.SCHEMATIC;
        };
    }

    private int sanitizeReadTimeout(long timeout) {
        if (timeout <= 0) {
            plugin.getLogger().warn("Limbo {} has invalid read-timeout={} (must be > 0). Using 30000ms.", name, timeout);
            return 30000;
        }

        if (timeout > Integer.MAX_VALUE) {
            plugin.getLogger().warn("Limbo {} read-timeout too large ({}). Capping to {}.", name, timeout, Integer.MAX_VALUE);
            return Integer.MAX_VALUE;
        }

        return (int) timeout;
    }

    public String getName() {
        return name;
    }

    public LimboServerConfig getConfig() {
        return config;
    }

    public Limbo getLimbo() {
        return limbo;
    }

    public VirtualWorld getWorld() {
        return world;
    }
}
