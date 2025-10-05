package wiki.creeper.creeperGiftBox;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import wiki.creeper.creeperGiftBox.api.GiftBoxAPI;
import wiki.creeper.creeperGiftBox.api.GiftBoxAPIImpl;
import wiki.creeper.creeperGiftBox.command.GiftCommand;
import wiki.creeper.creeperGiftBox.config.ConfigManager;
import wiki.creeper.creeperGiftBox.database.DatabaseManager;
import wiki.creeper.creeperGiftBox.gui.GiftBoxGUI;
import wiki.creeper.creeperGiftBox.listener.GUIListener;
import wiki.creeper.creeperGiftBox.listener.PlayerListener;
import wiki.creeper.creeperGiftBox.task.ExpirationTask;

/**
 * CreeperGiftBox - A comprehensive gift box system for Minecraft servers
 * 
 * This plugin provides a database-backed gift system where players can send
 * and receive items as gifts. Features include expiration support, GUI interface,
 * async operations, and a public API for integration with other plugins.
 * 
 * @author CreeperGiftBox
 * @version 1.0.2
 */
public final class CreeperGiftBox extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private GiftBoxGUI giftBoxGUI;
    private GiftBoxAPI giftBoxAPI;

    /**
     * Called when the plugin is enabled.
     * Initializes all managers, registers listeners, and starts scheduled tasks.
     * The initialization is done asynchronously to prevent server lag.
     */
    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);

        if (!ensureDatabaseDriver()) {
            getLogger().severe("MySQL JDBC driver not found. Dependent plugins may fail to connect to the database.");
        }

        try {
            databaseManager = new DatabaseManager(this, configManager);
        } catch (RuntimeException e) {
            getLogger().severe("Failed to create database manager. Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        giftBoxAPI = new GiftBoxAPIImpl(this, databaseManager);
        Bukkit.getServicesManager().register(GiftBoxAPI.class, giftBoxAPI, this, ServicePriority.Normal);
        getLogger().info("GiftBox API registered. Awaiting database initialization...");
        
        databaseManager.initialize().thenAccept(success -> {
            if (!success) {
                getLogger().severe("Failed to initialize database. Plugin will be disabled.");
                getServer().getScheduler().runTask(this, () -> getServer().getPluginManager().disablePlugin(this));
                return;
            }
            
            getServer().getScheduler().runTask(this, () -> {
                giftBoxGUI = new GiftBoxGUI(this);

                GiftCommand giftCommand = new GiftCommand(this);
                getCommand("우편함").setExecutor(giftCommand);
                getCommand("우편함").setTabCompleter(giftCommand);

                getServer().getPluginManager().registerEvents(new GUIListener(this), this);
                getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

                long interval = configManager.getExpirationCheckInterval() * 20;
                new ExpirationTask(this, databaseManager).runTaskTimerAsynchronously(this, 20L * 60, interval);

                getLogger().info("CreeperGiftBox has been enabled successfully!");
            });
        }).exceptionally(throwable -> {
            getLogger().severe("Unexpected error during plugin initialization: " + throwable.getMessage());
            getServer().getScheduler().runTask(this, () -> getServer().getPluginManager().disablePlugin(this));
            return null;
        });
    }

    /**
     * Called when the plugin is disabled.
     * Properly closes database connections and unregisters services.
     */
    @Override
    public void onDisable() {
        if (giftBoxAPI != null) {
            Bukkit.getServicesManager().unregister(GiftBoxAPI.class, giftBoxAPI);
            giftBoxAPI = null;
        }

        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("CreeperGiftBox has been disabled.");
    }

    /**
     * Gets the configuration manager for this plugin.
     * 
     * @return The ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the database manager for this plugin.
     * 
     * @return The DatabaseManager instance
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Gets the GUI manager for the gift box interface.
     * 
     * @return The GiftBoxGUI instance
     */
    public GiftBoxGUI getGiftBoxGUI() {
        return giftBoxGUI;
    }

    /**
     * 이 플러그인의 공용 API 인스턴스를 반환합니다.
     * @return GiftBoxAPI 인스턴스.
     */
    public GiftBoxAPI getGiftBoxAPI() {
        return giftBoxAPI;
    }

    private boolean ensureDatabaseDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver", true, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
