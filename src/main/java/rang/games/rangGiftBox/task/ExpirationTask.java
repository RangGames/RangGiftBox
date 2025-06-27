package rang.games.rangGiftBox.task;

import org.bukkit.scheduler.BukkitRunnable;
import rang.games.rangGiftBox.RangGiftBox;
import rang.games.rangGiftBox.database.DatabaseManager;

public class ExpirationTask extends BukkitRunnable {

    private final RangGiftBox plugin;
    private final DatabaseManager databaseManager;

    public ExpirationTask(RangGiftBox plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public void run() {
        plugin.getLogger().info("Checking for expired gifts...");
        databaseManager.findAndRemoveExpiredGifts().thenRun(() -> {
            plugin.getLogger().info("Expired gift check complete.");
        });
    }
}