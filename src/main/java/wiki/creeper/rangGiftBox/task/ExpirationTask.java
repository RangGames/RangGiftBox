package wiki.creeper.rangGiftBox.task;

import org.bukkit.scheduler.BukkitRunnable;
import wiki.creeper.rangGiftBox.RangGiftBox;
import wiki.creeper.rangGiftBox.database.DatabaseManager;
import wiki.creeper.rangGiftBox.util.DebugLogger;

public class ExpirationTask extends BukkitRunnable {

    private final RangGiftBox plugin;
    private final DatabaseManager databaseManager;
    private final DebugLogger debugLogger;

    public ExpirationTask(RangGiftBox plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.debugLogger = new DebugLogger(plugin);
    }

    @Override
    public void run() {
        debugLogger.debug("Starting expired gift check...");
        //plugin.getLogger().info("Checking for expired gifts...");
        
        databaseManager.findAndRemoveExpiredGifts().thenRun(() -> {
            debugLogger.debug("Expired gift check completed successfully");
            //plugin.getLogger().info("Expired gift check complete.");
        }).exceptionally(throwable -> {
            debugLogger.debugException("Error during expired gift check", throwable);
            plugin.getLogger().severe("Failed to check for expired gifts: " + throwable.getMessage());
            return null;
        });
    }
}