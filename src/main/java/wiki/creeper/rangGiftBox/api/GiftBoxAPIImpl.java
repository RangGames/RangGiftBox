package wiki.creeper.rangGiftBox.api;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import wiki.creeper.rangGiftBox.RangGiftBox;
import wiki.creeper.rangGiftBox.database.DatabaseManager;
import wiki.creeper.rangGiftBox.model.Gift;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.Objects;
import java.util.logging.Level;

public class GiftBoxAPIImpl implements GiftBoxAPI {

    private final RangGiftBox plugin;
    private final DatabaseManager databaseManager;

    public GiftBoxAPIImpl(RangGiftBox plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public CompletableFuture<Void> sendGift(UUID targetPlayerUUID, ItemStack itemStack, String senderName, long expireSeconds) {
        // Validate parameters
        Objects.requireNonNull(targetPlayerUUID, "targetPlayerUUID cannot be null");
        Objects.requireNonNull(itemStack, "itemStack cannot be null");
        Objects.requireNonNull(senderName, "senderName cannot be null");
        
        if (itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Cannot send an empty or invalid item")
            );
        }
        
        if (senderName.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Sender name cannot be empty")
            );
        }
        
        if (expireSeconds < -1) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Expire seconds must be -1 (never expire) or greater")
            );
        }
        
        // Sanitize sender name to prevent potential exploits
        String trimmedSender = senderName.trim();
        String sanitizedSender = trimmedSender.substring(0, Math.min(trimmedSender.length(), 100));

        long currentTime = System.currentTimeMillis();
        long expireTime = -1;
        if (expireSeconds > 0) {
            try {
                long expireMillis = Math.multiplyExact(expireSeconds, 1000L);
                expireTime = Math.addExact(currentTime, expireMillis);
            } catch (ArithmeticException ex) {
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Expire seconds is too large", ex)
                );
            }
        }

        Gift gift = new Gift(
                UUID.randomUUID().toString(),
                targetPlayerUUID,
                itemStack.clone(), // Clone to prevent external modification
                sanitizedSender,
                currentTime,
                expireTime
        );
        
        return databaseManager.addGift(gift).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, 
                "Failed to send gift to player " + targetPlayerUUID, throwable);
            throw new CompletionException("Failed to send gift", throwable);
        });
    }

    @Override
    public CompletableFuture<List<Gift>> getPlayerGifts(UUID playerUUID, int limit) {
        Objects.requireNonNull(playerUUID, "playerUUID cannot be null");
        
        if (limit <= 0) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("Limit must be greater than 0")
            );
        }
        
        // Cap the limit to prevent excessive database queries
        int cappedLimit = Math.min(limit, 100);
        
        return databaseManager.getGifts(playerUUID, cappedLimit)
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, 
                    "Failed to retrieve gifts for player " + playerUUID, throwable);
                // Return empty list on failure instead of propagating the error
                return new ArrayList<>();
            });
    }

    @Override
    public CompletableFuture<Integer> getPlayerGiftCount(UUID playerUUID) {
        Objects.requireNonNull(playerUUID, "playerUUID cannot be null");
        
        return databaseManager.getGiftCount(playerUUID)
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, 
                    "Failed to get gift count for player " + playerUUID, throwable);
                // Return 0 on failure
                return 0;
            });
    }
}
