package rang.games.rangGiftBox.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import rang.games.rangGiftBox.RangGiftBox;
import rang.games.rangGiftBox.config.ConfigManager;
import rang.games.rangGiftBox.event.GiftExpiredEvent;
import rang.games.rangGiftBox.event.GiftSentEvent;
import rang.games.rangGiftBox.model.Gift;
import rang.games.rangGiftBox.util.ItemSerializer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

/**
 * Manages all database operations for the RangGiftBox plugin.
 * 
 * This class handles database connections using HikariCP connection pooling,
 * performs async database operations, and manages the gift and log tables.
 * All public methods return CompletableFuture for non-blocking operations.
 * 
 * @author RangGiftBox Team
 */
public class DatabaseManager {

    private static final String TABLE_PRESENT = "present";
    private static final String TABLE_PRESENT_LOG = "present_log";
    
    private final RangGiftBox plugin;
    private final HikariDataSource dataSource;
    private volatile boolean isInitialized = false;

    /**
     * Creates a new DatabaseManager with HikariCP connection pool.
     * 
     * @param plugin The main plugin instance
     * @param configManager The configuration manager for database settings
     * @throws RuntimeException if database initialization fails
     */
    public DatabaseManager(RangGiftBox plugin, ConfigManager configManager) {
        this.plugin = plugin;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + configManager.getDbHost() + ":" + configManager.getDbPort() + "/" + configManager.getDbName());
        config.setUsername(configManager.getDbUser());
        config.setPassword(configManager.getDbPassword());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setPoolName("RangGiftBox-Pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        try {
            this.dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Initializes the database tables asynchronously.
     * Creates the present and present_log tables if they don't exist.
     * 
     * @return CompletableFuture<Boolean> true if initialization succeeds, false otherwise
     */
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            if (isInitialized) {
                plugin.getLogger().warning("Database already initialized, skipping...");
                return true;
            }
            
            try (Connection connection = dataSource.getConnection()) {
                String createPresentTable = "CREATE TABLE IF NOT EXISTS present (" +
                        "ID VARCHAR(36) PRIMARY KEY, " +
                        "UUID VARCHAR(36) NOT NULL, " +
                        "ItemStack TEXT NOT NULL, " +
                        "Count INT NOT NULL, " +
                        "Sender VARCHAR(255) NOT NULL, " +
                        "TimeStamp BIGINT NOT NULL, " +
                        "ExpireStamp BIGINT NOT NULL, " +
                        "INDEX uuid_index (UUID));";
                try (PreparedStatement ps = connection.prepareStatement(createPresentTable)) {
                    ps.execute();
                }

                String createLogTable = "CREATE TABLE IF NOT EXISTS present_log (" +
                        "LogID INT AUTO_INCREMENT PRIMARY KEY, " +
                        "GiftID VARCHAR(36) NOT NULL, " +
                        "PlayerUUID VARCHAR(36) NOT NULL, " +
                        "ItemStack TEXT NOT NULL, " +
                        "Count INT NOT NULL, " +
                        "Sender VARCHAR(255) NOT NULL, " +
                        "Result INT NOT NULL, " +
                        "TimeStamp BIGINT NOT NULL);";
                try (PreparedStatement ps = connection.prepareStatement(createLogTable)) {
                    ps.execute();
                }
                
                isInitialized = true;
                plugin.getLogger().info("Database tables initialized successfully");
                return true;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not initialize database tables!", e);
                return false;
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "Unexpected error during database initialization", throwable);
            return false;
        });
    }

    /**
     * Closes the database connection pool.
     * Should be called when the plugin is disabled.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
                plugin.getLogger().info("Database connection pool closed successfully");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error closing database connection pool", e);
            }
        }
        isInitialized = false;
    }

    /**
     * Adds a new gift to the database asynchronously.
     * Also logs the action and fires a GiftSentEvent.
     * 
     * @param gift The gift to add
     * @return CompletableFuture<Void> that completes when the gift is added
     * @throws CompletionException if the database operation fails
     */
    public CompletableFuture<Void> addGift(Gift gift) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement("INSERT INTO present (ID, UUID, ItemStack, Count, Sender, TimeStamp, ExpireStamp) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, gift.getId());
                ps.setString(2, gift.getPlayerUUID().toString());
                ps.setString(3, ItemSerializer.serialize(gift.getItemStack()));
                ps.setInt(4, gift.getItemStack().getAmount());
                ps.setString(5, gift.getSender());
                ps.setLong(6, gift.getTimestamp());
                ps.setLong(7, gift.getExpireStamp());
                int affectedRows = ps.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Failed to insert gift, no rows affected");
                }
                
                logAction(gift, LogResult.SENT).exceptionally(ex -> {
                    plugin.getLogger().log(Level.WARNING, "Failed to log gift sent action", ex);
                    return null;
                });
                
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new GiftSentEvent(gift)));
            } catch (SQLException | IllegalStateException e) {
                plugin.getLogger().log(Level.SEVERE, "Error adding gift to database for player " + gift.getPlayerUUID(), e);
                throw new CompletionException("Failed to add gift", e);
            }
        });
    }

    /**
     * Retrieves gifts for a specific player asynchronously.
     * Only returns non-expired gifts, ordered by timestamp.
     * 
     * @param playerUUID The player's UUID
     * @param limit Maximum number of gifts to retrieve
     * @return CompletableFuture<List<Gift>> containing the player's gifts
     * @throws CompletionException if the database operation fails
     */
    public CompletableFuture<List<Gift>> getGifts(UUID playerUUID, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<Gift> gifts = new ArrayList<>();
            String query = "SELECT * FROM present WHERE UUID = ? AND (ExpireStamp = -1 OR ExpireStamp > ?) ORDER BY TimeStamp ASC LIMIT ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, playerUUID.toString());
                ps.setLong(2, System.currentTimeMillis());
                ps.setInt(3, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        gifts.add(new Gift(
                                rs.getString("ID"),
                                UUID.fromString(rs.getString("UUID")),
                                ItemSerializer.deserialize(rs.getString("ItemStack")),
                                rs.getString("Sender"),
                                rs.getLong("TimeStamp"),
                                rs.getLong("ExpireStamp")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error while getting gifts for player " + playerUUID, e);
                throw new CompletionException("Failed to retrieve gifts", e);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Deserialization error while getting gifts for player " + playerUUID, e);
                throw new CompletionException("Failed to deserialize gift items", e);
            }
            return gifts;
        });
    }

    /**
     * Gets the count of non-expired gifts for a player asynchronously.
     * 
     * @param playerUUID The player's UUID
     * @return CompletableFuture<Integer> containing the gift count
     * @throws CompletionException if the database operation fails
     */
    public CompletableFuture<Integer> getGiftCount(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT COUNT(*) FROM present WHERE UUID = ? AND (ExpireStamp = -1 OR ExpireStamp > ?)";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, playerUUID.toString());
                ps.setLong(2, System.currentTimeMillis());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error getting gift count for player " + playerUUID, e);
                throw new CompletionException("Failed to get gift count", e);
            }
            return 0;
        });
    }

    /**
     * Deletes a single gift from the database asynchronously.
     * 
     * @param giftId The unique ID of the gift to delete
     * @return CompletableFuture<Boolean> true if the gift was deleted, false if not found
     * @throws CompletionException if the database operation fails
     */
    public CompletableFuture<Boolean> deleteGift(String giftId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement("DELETE FROM " + TABLE_PRESENT + " WHERE ID = ?")) {
                ps.setString(1, giftId);
                int affectedRows = ps.executeUpdate();
                return affectedRows > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error deleting gift " + giftId + " from database", e);
                throw new CompletionException("Failed to delete gift", e);
            }
        });
    }

    /**
     * Deletes multiple gifts from the database asynchronously.
     * Uses batch operations for better performance.
     * 
     * @param giftIds List of gift IDs to delete
     * @return CompletableFuture<Integer> number of gifts actually deleted
     * @throws CompletionException if the database operation fails
     */
    public CompletableFuture<Integer> deleteGifts(List<String> giftIds) {
        return CompletableFuture.supplyAsync(() -> {
            if (giftIds.isEmpty()) return 0;
            
            // Use batch delete for better performance
            String query = "DELETE FROM " + TABLE_PRESENT + " WHERE ID = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(query)) {
                
                connection.setAutoCommit(false);
                try {
                    int totalDeleted = 0;
                    for (String giftId : giftIds) {
                        ps.setString(1, giftId);
                        ps.addBatch();
                        
                        // Execute batch every 100 items
                        if (totalDeleted % 100 == 0 && totalDeleted > 0) {
                            int[] results = ps.executeBatch();
                            for (int result : results) {
                                if (result > 0) totalDeleted++;
                            }
                        }
                    }
                    
                    // Execute remaining batch
                    int[] results = ps.executeBatch();
                    for (int result : results) {
                        if (result > 0) totalDeleted++;
                    }
                    
                    connection.commit();
                    return totalDeleted;
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error deleting " + giftIds.size() + " gifts from database", e);
                throw new CompletionException("Failed to delete gifts", e);
            }
        });
    }

    /**
     * Logs a gift action to the present_log table asynchronously.
     * This method does not throw exceptions to prevent disrupting main operations.
     * 
     * @param gift The gift involved in the action
     * @param result The result of the action (SENT, CLAIMED, EXPIRED)
     * @return CompletableFuture<Void> that completes when logged
     */
    public CompletableFuture<Void> logAction(Gift gift, LogResult result) {
        return CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO present_log (GiftID, PlayerUUID, ItemStack, Count, Sender, Result, TimeStamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, gift.getId());
                ps.setString(2, gift.getPlayerUUID().toString());
                ps.setString(3, ItemSerializer.serialize(gift.getItemStack()));
                ps.setInt(4, gift.getItemStack().getAmount());
                ps.setString(5, gift.getSender());
                ps.setInt(6, result.getValue());
                ps.setLong(7, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error logging gift action for gift " + gift.getId(), e);
                // Don't throw exception for logging failures - they shouldn't break the main operation
            } catch (IllegalStateException e) {
                plugin.getLogger().log(Level.WARNING, "Error serializing item for log: " + gift.getId(), e);
            }
        });
    }

    /**
     * Finds and removes all expired gifts from the database asynchronously.
     * Logs each expired gift and fires GiftExpiredEvent for each.
     * 
     * @return CompletableFuture<Void> that completes when all expired gifts are processed
     * @throws CompletionException if the database operation fails
     */
    public CompletableFuture<Void> findAndRemoveExpiredGifts() {
        return CompletableFuture.runAsync(() -> {
            List<Gift> expiredGifts = new ArrayList<>();
            String selectQuery = "SELECT * FROM present WHERE ExpireStamp != -1 AND ExpireStamp <= ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement psSelect = connection.prepareStatement(selectQuery)) {
                psSelect.setLong(1, System.currentTimeMillis());
                try (ResultSet rs = psSelect.executeQuery()) {
                    while (rs.next()) {
                        expiredGifts.add(new Gift(
                                rs.getString("ID"),
                                UUID.fromString(rs.getString("UUID")),
                                ItemSerializer.deserialize(rs.getString("ItemStack")),
                                rs.getString("Sender"),
                                rs.getLong("TimeStamp"),
                                rs.getLong("ExpireStamp")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Database error while finding expired gifts", e);
                throw new CompletionException("Failed to find expired gifts", e);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Deserialization error while processing expired gifts", e);
                return;
            }

            if (expiredGifts.isEmpty()) return;

            // Log and fire events for expired gifts
            CompletableFuture<?>[] logFutures = expiredGifts.stream()
                .map(gift -> logAction(gift, LogResult.EXPIRED)
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, 
                        () -> Bukkit.getPluginManager().callEvent(new GiftExpiredEvent(gift))))
                    .exceptionally(ex -> {
                        plugin.getLogger().log(Level.WARNING, "Failed to process expired gift " + gift.getId(), ex);
                        return null;
                    }))
                .toArray(CompletableFuture[]::new);
            
            // Wait for all logging operations to complete
            CompletableFuture.allOf(logFutures).join();

            String deleteQuery = "DELETE FROM present WHERE ExpireStamp != -1 AND ExpireStamp <= ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement psDelete = connection.prepareStatement(deleteQuery)) {
                psDelete.setLong(1, System.currentTimeMillis());
                int deletedCount = psDelete.executeUpdate();
                if (deletedCount > 0) {
                    plugin.getLogger().info("Removed " + deletedCount + " expired gifts from database");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error removing expired gifts from database", e);
                throw new CompletionException("Failed to remove expired gifts", e);
            }
        });
    }
}