package rang.games.rangGiftBox;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import rang.games.rangGiftBox.api.GiftBoxAPI;
import rang.games.rangGiftBox.api.GiftBoxAPIImpl;
import rang.games.rangGiftBox.command.GiftCommand;
import rang.games.rangGiftBox.config.ConfigManager;
import rang.games.rangGiftBox.database.DatabaseManager;
import rang.games.rangGiftBox.gui.GiftBoxGUI;
import rang.games.rangGiftBox.listener.GUIListener;
import rang.games.rangGiftBox.listener.PlayerListener;
import rang.games.rangGiftBox.task.ExpirationTask;

public final class RangGiftBox extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private GiftBoxGUI giftBoxGUI;
    private GiftBoxAPI giftBoxAPI;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this, configManager);
        databaseManager.initialize();

        giftBoxGUI = new GiftBoxGUI(this);

        giftBoxAPI = new GiftBoxAPIImpl(this, databaseManager);
        Bukkit.getServicesManager().register(GiftBoxAPI.class, giftBoxAPI, this, ServicePriority.Normal);

        GiftCommand giftCommand = new GiftCommand(this);
        getCommand("우편함").setExecutor(giftCommand);
        getCommand("우편함").setTabCompleter(giftCommand);

        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        long interval = configManager.getExpirationCheckInterval() * 20;
        new ExpirationTask(this, databaseManager).runTaskTimerAsynchronously(this, 20L * 60, interval);

        getLogger().info("RangGiftBox has been enabled!");
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregister(GiftBoxAPI.class, giftBoxAPI);

        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("RangGiftBox has been disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

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
}