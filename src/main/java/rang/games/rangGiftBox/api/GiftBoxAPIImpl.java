package rang.games.rangGiftBox.api;

import org.bukkit.inventory.ItemStack;
import rang.games.rangGiftBox.RangGiftBox;
import rang.games.rangGiftBox.database.DatabaseManager;
import rang.games.rangGiftBox.model.Gift;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GiftBoxAPIImpl implements GiftBoxAPI {

    private final RangGiftBox plugin;
    private final DatabaseManager databaseManager;

    public GiftBoxAPIImpl(RangGiftBox plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public CompletableFuture<Void> sendGift(UUID targetPlayerUUID, ItemStack itemStack, String senderName, long expireSeconds) {
        long currentTime = System.currentTimeMillis();
        long expireTime = (expireSeconds > 0) ? currentTime + (expireSeconds * 1000) : -1;

        Gift gift = new Gift(
                UUID.randomUUID().toString(),
                targetPlayerUUID,
                itemStack.clone(),
                senderName,
                currentTime,
                expireTime
        );
        return databaseManager.addGift(gift);
    }

    @Override
    public CompletableFuture<List<Gift>> getPlayerGifts(UUID playerUUID, int limit) {
        return databaseManager.getGifts(playerUUID, limit);
    }

    @Override
    public CompletableFuture<Integer> getPlayerGiftCount(UUID playerUUID) {
        return databaseManager.getGiftCount(playerUUID);
    }
}