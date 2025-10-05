package wiki.creeper.creeperGiftBox.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import wiki.creeper.creeperGiftBox.CreeperGiftBox;
import wiki.creeper.creeperGiftBox.config.ConfigManager;
import wiki.creeper.creeperGiftBox.database.DatabaseManager;
import wiki.creeper.creeperGiftBox.util.SchedulerUtil;

import java.util.logging.Level;

public class PlayerListener implements Listener {

    private final CreeperGiftBox plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;

    public PlayerListener(CreeperGiftBox plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        databaseManager.getGiftCount(player.getUniqueId())
                .thenAcceptAsync(count -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (count > 0) {
                        player.sendMessage(configManager.getMessage("join-notification", "%amount%", String.valueOf(count)));
                    }
                }, SchedulerUtil.syncExecutor(plugin))
                .exceptionally(throwable -> {
                    plugin.getLogger().log(Level.WARNING,
                            "Failed to fetch gift count for " + player.getName(), throwable);
                    return null;
                });
    }
}
