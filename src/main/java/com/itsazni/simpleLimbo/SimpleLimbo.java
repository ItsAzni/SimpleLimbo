package com.itsazni.simpleLimbo;

import com.google.inject.Inject;
import com.itsazni.simpleLimbo.bridge.VelocityAliasBridge;
import com.itsazni.simpleLimbo.command.SimpleLimboCommand;
import com.itsazni.simpleLimbo.config.ConfigLoader;
import com.itsazni.simpleLimbo.compat.ServerConnectionInjector;
import com.itsazni.simpleLimbo.config.Settings;
import com.itsazni.simpleLimbo.display.DisplayManager;
import com.itsazni.simpleLimbo.limbo.LimboManager;
import com.itsazni.simpleLimbo.listener.PlayerListener;
import com.itsazni.simpleLimbo.trigger.TriggerManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.elytrium.limboapi.api.LimboFactory;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "simplelimbo",
        name = "SimpleLimbo",
        version = BuildConstants.VERSION,
        url = "https://github.com/ItsAzni",
        authors = {"ItsAzni"},
        dependencies = {@Dependency(id = "limboapi")}
)
public class SimpleLimbo {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigLoader configLoader;
    private Settings settings;
    private LimboManager limboManager;
    private DisplayManager displayManager;
    private TriggerManager triggerManager;
    private VelocityAliasBridge velocityAliasBridge;

    @Inject
    public SimpleLimbo(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.configLoader = new ConfigLoader(dataDirectory, logger);
        this.settings = configLoader.load();

        // Initialize fake server connection injector for auth plugin compatibility
        ServerConnectionInjector.init(logger);

        LimboFactory factory = (LimboFactory) server.getPluginManager()
                .getPlugin("limboapi")
                .flatMap(PluginContainer::getInstance)
                .orElseThrow(() -> new IllegalStateException("LimboAPI plugin not found"));

        this.displayManager = new DisplayManager(this);
        this.limboManager = new LimboManager(this, factory);
        this.triggerManager = new TriggerManager(this);
        this.velocityAliasBridge = new VelocityAliasBridge(this);

        this.limboManager.loadAll();
        this.velocityAliasBridge.registerAliases();
        this.triggerManager.start();

        this.server.getEventManager().register(this, new PlayerListener(this));
        this.server.getCommandManager().register(
                this.server.getCommandManager().metaBuilder("simplelimbo").plugin(this).build(),
                new SimpleLimboCommand(this)
        );

        this.logger.info("SimpleLimbo initialized. Loaded {} limbo server(s)", this.limboManager.getLimboNames().size());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (displayManager != null) {
            displayManager.clearAll();
        }
        if (triggerManager != null) {
            triggerManager.stop();
        }
        if (velocityAliasBridge != null) {
            velocityAliasBridge.unregisterAliases();
        }
    }

    public void reload() {
        this.settings = configLoader.load();
        this.displayManager.clearAll();
        this.triggerManager.stop();
        this.velocityAliasBridge.unregisterAliases();
        this.limboManager.reload();
        this.velocityAliasBridge.registerAliases();
        this.triggerManager.start();

        this.logger.info("SimpleLimbo configuration reloaded");
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public Settings getSettings() {
        return settings;
    }

    public LimboManager getLimboManager() {
        return limboManager;
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    public TriggerManager getTriggerManager() {
        return triggerManager;
    }

    public VelocityAliasBridge getVelocityAliasBridge() {
        return velocityAliasBridge;
    }
}
