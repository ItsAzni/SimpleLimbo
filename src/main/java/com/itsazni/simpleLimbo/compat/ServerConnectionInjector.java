package com.itsazni.simpleLimbo.compat;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.netty.channel.embedded.EmbeddedChannel;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Provides compatibility with auth plugins by injecting a server connection stub.
 * 
 * When players are in limbo, they have no backend server connection. This causes
 * auth plugins (like JPremium) that call player.getCurrentServer() to fail.
 * This class injects a VelocityServerConnection backed by an EmbeddedChannel
 * to satisfy those checks.
 * 
 * Note: Uses reflection to access internal Velocity APIs and may require updates
 * when Velocity internals change.
 */
public class ServerConnectionInjector {

    // Internal Velocity class names
    private static final String CONNECTED_PLAYER_CLASS = "com.velocitypowered.proxy.connection.client.ConnectedPlayer";
    private static final String VELOCITY_SERVER_CLASS = "com.velocitypowered.proxy.VelocityServer";
    private static final String VELOCITY_REGISTERED_SERVER_CLASS = "com.velocitypowered.proxy.server.VelocityRegisteredServer";
    private static final String VELOCITY_SERVER_CONNECTION_CLASS = "com.velocitypowered.proxy.connection.backend.VelocityServerConnection";
    private static final String MINECRAFT_CONNECTION_CLASS = "com.velocitypowered.proxy.connection.MinecraftConnection";

    // Cached reflection objects
    private static Class<?> connectedPlayerClass;
    private static Class<?> velocityServerClass;
    private static Class<?> velocityRegisteredServerClass;
    private static Class<?> velocityServerConnectionClass;
    private static Class<?> minecraftConnectionClass;
    
    private static Constructor<?> serverConnectionConstructor;
    private static Constructor<?> minecraftConnectionConstructor;
    private static Method setConnectedServerMethod;
    private static Method getConnectedServerMethod;
    private static Field hasCompletedJoinField;
    private static Field connectionField; // VelocityServerConnection.connection

    private static boolean initialized = false;
    private static boolean available = false;

    /**
     * Initialize the reflective access to Velocity internal classes.
     * This should be called once at plugin startup.
     */
    public static synchronized void init(Logger logger) {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            // Load internal classes
            connectedPlayerClass = Class.forName(CONNECTED_PLAYER_CLASS);
            velocityServerClass = Class.forName(VELOCITY_SERVER_CLASS);
            velocityRegisteredServerClass = Class.forName(VELOCITY_REGISTERED_SERVER_CLASS);
            velocityServerConnectionClass = Class.forName(VELOCITY_SERVER_CONNECTION_CLASS);
            minecraftConnectionClass = Class.forName(MINECRAFT_CONNECTION_CLASS);

            // Get VelocityServerConnection constructor:
            // public VelocityServerConnection(VelocityRegisteredServer registeredServer,
            //     @Nullable VelocityRegisteredServer previousServer,
            //     ConnectedPlayer proxyPlayer, VelocityServer server)
            serverConnectionConstructor = velocityServerConnectionClass.getConstructor(
                    velocityRegisteredServerClass,  // registeredServer
                    velocityRegisteredServerClass,  // previousServer (nullable)
                    connectedPlayerClass,           // proxyPlayer
                    velocityServerClass             // server
            );

            // Get MinecraftConnection constructor:
            // public MinecraftConnection(Channel channel, VelocityServer server)
            minecraftConnectionConstructor = minecraftConnectionClass.getConstructor(
                    io.netty.channel.Channel.class,
                    velocityServerClass
            );

            // Get setConnectedServer method (public)
            setConnectedServerMethod = connectedPlayerClass.getMethod(
                    "setConnectedServer", velocityServerConnectionClass);

            // Get getConnectedServer method (public, returns VelocityServerConnection)
            getConnectedServerMethod = connectedPlayerClass.getMethod("getConnectedServer");

            // Get hasCompletedJoin field in VelocityServerConnection
            hasCompletedJoinField = velocityServerConnectionClass.getDeclaredField("hasCompletedJoin");
            hasCompletedJoinField.setAccessible(true);

            // Get connection field in VelocityServerConnection
            connectionField = velocityServerConnectionClass.getDeclaredField("connection");
            connectionField.setAccessible(true);

            available = true;
            logger.info("ServerConnectionInjector initialized successfully - using real VelocityServerConnection with fake MinecraftConnection");
        } catch (ClassNotFoundException e) {
            logger.warn("Could not find Velocity internal class: {} - fake server injection not available", e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.warn("Could not find required method: {} - fake server injection not available", e.getMessage());
        } catch (NoSuchFieldException e) {
            logger.warn("Could not find required field: {} - fake server injection not available", e.getMessage());
        } catch (Exception e) {
            logger.warn("Failed to initialize ServerConnectionInjector: {}", e.getMessage());
        }
    }

    /**
     * Check if server connection injection is available.
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * Inject a VelocityServerConnection into the player, making getCurrentServer() return a valid connection.
     * Also injects a fake MinecraftConnection backed by EmbeddedChannel to satisfy ensureConnected() checks.
     * 
     * @param player The player to inject into
     * @param fakeServer The RegisteredServer to use as the "connected" server
     * @param proxyServer The Velocity proxy server instance
     * @param logger Logger for debug output
     * @return true if injection was successful
     */
    public static boolean injectFakeServer(Player player, RegisteredServer fakeServer, ProxyServer proxyServer, Logger logger) {
        if (!available) {
            logger.warn("ServerConnectionInjector not available");
            return false;
        }

        try {
            // Only inject if player doesn't already have a server connection
            if (player.getCurrentServer().isPresent()) {
                logger.debug("Player {} already has a server connection, skipping injection", player.getUsername());
                return false;
            }

            // Cast to internal types
            // ProxyServer -> VelocityServer
            if (!velocityServerClass.isInstance(proxyServer)) {
                logger.warn("ProxyServer is not a VelocityServer instance");
                return false;
            }

            // RegisteredServer -> VelocityRegisteredServer
            if (!velocityRegisteredServerClass.isInstance(fakeServer)) {
                logger.warn("RegisteredServer is not a VelocityRegisteredServer instance");
                return false;
            }

            // Player -> ConnectedPlayer
            if (!connectedPlayerClass.isInstance(player)) {
                logger.warn("Player is not a ConnectedPlayer instance");
                return false;
            }

            // Create VelocityServerConnection instance
            Object serverConnection = serverConnectionConstructor.newInstance(
                    fakeServer,     // registeredServer (VelocityRegisteredServer)
                    null,           // previousServer (nullable)
                    player,         // proxyPlayer (ConnectedPlayer)
                    proxyServer     // server (VelocityServer)
            );

            // Create fake MinecraftConnection with EmbeddedChannel
            EmbeddedChannel fakeChannel = new EmbeddedChannel();
            Object minecraftConnection = minecraftConnectionConstructor.newInstance(
                    fakeChannel,    // channel (EmbeddedChannel - Netty test channel)
                    proxyServer     // server (VelocityServer)
            );

            // Inject the MinecraftConnection into VelocityServerConnection
            connectionField.set(serverConnection, minecraftConnection);

            // Set hasCompletedJoin = true to prevent issues
            hasCompletedJoinField.setBoolean(serverConnection, true);

            // Use public setConnectedServer method
            setConnectedServerMethod.invoke(player, serverConnection);

            logger.info("Injected VelocityServerConnection '{}' for player {} in limbo (with fake MinecraftConnection)", 
                    fakeServer.getServerInfo().getName(), player.getUsername());
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to inject server connection for {}: {}", player.getUsername(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Remove the injected ServerConnection from the player.
     * This should be called when player leaves limbo.
     * 
     * @param player The player to clear
     * @param logger Logger for debug output
     * @return true if removal was successful
     */
    public static boolean clearFakeServer(Player player, Logger logger) {
        if (!available) {
            return false;
        }

        try {
            // Check if player has a connection
            Object currentConnection = getConnectedServerMethod.invoke(player);
            if (currentConnection == null) {
                return false;
            }

            // Close the fake channel if present
            try {
                Object minecraftConn = connectionField.get(currentConnection);
                if (minecraftConn != null) {
                    // Get channel field from MinecraftConnection and close it
                    Field channelField = minecraftConnectionClass.getDeclaredField("channel");
                    channelField.setAccessible(true);
                    Object channel = channelField.get(minecraftConn);
                    if (channel instanceof EmbeddedChannel) {
                        ((EmbeddedChannel) channel).close();
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not close fake channel: {}", e.getMessage());
            }

            // Clear by calling setConnectedServer(null)
            setConnectedServerMethod.invoke(player, (Object) null);
            logger.debug("Cleared fake server connection for player {}", player.getUsername());
            return true;
            
        } catch (Exception e) {
            logger.warn("Failed to clear fake server for {}: {}", player.getUsername(), e.getMessage());
            return false;
        }
    }

    /**
     * Check if the player currently has a server connection.
     */
    public static boolean hasServerConnection(Player player) {
        if (!available) {
            return false;
        }

        try {
            Object currentConnection = getConnectedServerMethod.invoke(player);
            return currentConnection != null;
        } catch (Exception e) {
            return false;
        }
    }
}
