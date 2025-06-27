package rang.games.rangGiftBox.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import rang.games.rangGiftBox.RangGiftBox;
import rang.games.rangGiftBox.config.ConfigManager;
import rang.games.rangGiftBox.database.DatabaseManager;

public class PlayerListener implements Listener {

    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;

    public PlayerListener(RangGiftBox plugin) {
        this.databaseManager = plugin.getDatabaseManager();
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        databaseManager.getGiftCount(player.getUniqueId()).thenAccept(count -> {
            if (count > 0) {
                player.sendMessage(configManager.getMessage("messages.join-notification", "%amount%", String.valueOf(count)));
            }
        });
    }
}