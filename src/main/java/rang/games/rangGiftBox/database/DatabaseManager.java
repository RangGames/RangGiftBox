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

public class DatabaseManager {

    private final RangGiftBox plugin;
    private final HikariDataSource dataSource;

    public DatabaseManager(RangGiftBox plugin, ConfigManager configManager) {
        this.plugin = plugin;
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + configManager.getDbHost() + ":" + configManager.getDbPort() + "/" + configManager.getDbName());
        config.setUsername(configManager.getDbUser());
        config.setPassword(configManager.getDbPassword());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setPoolName("RangGiftBox-Pool");
        config.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(config);
    }

    public void initialize() {
        CompletableFuture.runAsync(() -> {
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
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not initialize database tables!");
                e.printStackTrace();
            }
        });
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

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
                ps.executeUpdate();
                logAction(gift, LogResult.SENT);
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new GiftSentEvent(gift)));
            } catch (SQLException | IllegalStateException e) {
                plugin.getLogger().severe("Error adding gift to database: " + e.getMessage());
            }
        });
    }

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
            } catch (SQLException | IOException e) {
                plugin.getLogger().severe("Error getting gifts from database: " + e.getMessage());
            }
            return gifts;
        });
    }

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
                plugin.getLogger().severe("Error getting gift count from database: " + e.getMessage());
            }
            return 0;
        });
    }

    public CompletableFuture<Void> deleteGift(String giftId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement("DELETE FROM present WHERE ID = ?")) {
                ps.setString(1, giftId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error deleting gift from database: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> deleteGifts(List<String> giftIds) {
        return CompletableFuture.runAsync(() -> {
            if (giftIds.isEmpty()) return;
            String query = "DELETE FROM present WHERE ID IN (?" + ",?".repeat(giftIds.size() - 1) + ")";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(query)) {
                for (int i = 0; i < giftIds.size(); i++) {
                    ps.setString(i + 1, giftIds.get(i));
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error deleting multiple gifts from database: " + e.getMessage());
            }
        });
    }

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
            } catch (SQLException | IllegalStateException e) {
                plugin.getLogger().severe("Error logging gift action: " + e.getMessage());
            }
        });
    }

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
            } catch (SQLException | IOException e) {
                plugin.getLogger().severe("Error finding expired gifts: " + e.getMessage());
                return;
            }

            if (expiredGifts.isEmpty()) return;

            for (Gift gift : expiredGifts) {
                logAction(gift, LogResult.EXPIRED);
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new GiftExpiredEvent(gift)));
            }

            String deleteQuery = "DELETE FROM present WHERE ExpireStamp != -1 AND ExpireStamp <= ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement psDelete = connection.prepareStatement(deleteQuery)) {
                psDelete.setLong(1, System.currentTimeMillis());
                psDelete.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error removing expired gifts from database: " + e.getMessage());
            }
        });
    }
}