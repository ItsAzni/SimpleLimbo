package com.itsazni.simpleLimbo.config;

import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoader {

    private final Path dataDirectory;
    private final Logger logger;
    private Settings settings;

    public ConfigLoader(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public Settings load() {
        Path configFile = dataDirectory.resolve("config.yml");

        try {
            // Create data directory if it doesn't exist
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            // Copy default config if it doesn't exist
            if (!Files.exists(configFile)) {
                saveDefaultConfig(configFile);
            }

            // Load configuration
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configFile)
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();

            CommentedConfigurationNode node = loader.load();
            settings = node.get(Settings.class);

            if (settings == null) {
                logger.warn("Failed to load config, using defaults");
                settings = new Settings();
            }

            logger.info("Configuration loaded successfully");
            return settings;

        } catch (IOException e) {
            logger.error("Failed to load configuration: {}", e.getMessage(), e);
            settings = new Settings();
            return settings;
        }
    }

    public void save() {
        if (settings == null) {
            return;
        }

        Path configFile = dataDirectory.resolve("config.yml");

        try {
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configFile)
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();

            CommentedConfigurationNode node = loader.load();
            node.set(Settings.class, settings);
            loader.save(node);

            logger.info("Configuration saved successfully");

        } catch (ConfigurateException e) {
            logger.error("Failed to save configuration", e);
        }
    }

    private void saveDefaultConfig(Path configFile) {
        try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
            if (in != null) {
                Files.copy(in, configFile);
                logger.info("Default configuration created");
            } else {
                // No default config in resources, create from Settings class
                settings = new Settings();
                
                YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                        .path(configFile)
                        .nodeStyle(NodeStyle.BLOCK)
                        .build();

                CommentedConfigurationNode node = loader.load();
                node.set(Settings.class, settings);
                loader.save(node);
                
                logger.info("Generated default configuration");
            }
        } catch (IOException e) {
            logger.error("Failed to create default configuration", e);
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }
}
