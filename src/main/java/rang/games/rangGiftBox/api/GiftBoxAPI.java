package rang.games.rangGiftBox.api;

import org.bukkit.inventory.ItemStack;
import rang.games.rangGiftBox.model.Gift;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface GiftBoxAPI {

    /**
     * 특정 플레이어에게 아이템을 선물로 보냅니다.
     * 이 메서드는 비동기적으로 작동하며, 선물 지급이 완료되면 CompletableFuture가 완료됩니다.
     *
     * @param targetPlayerUUID 선물을 받을 플레이어의 UUID.
     * @param itemStack 보낼 아이템 (ItemStack).
     * @param senderName 발신인의 이름 (예: "Server", "Admin", 또는 플레이어 이름).
     * @param expireSeconds 만료 시간 (초 단위). -1은 영구 보관을 의미합니다.
     * @return 선물 지급 작업의 완료를 나타내는 CompletableFuture.
     */
    CompletableFuture<Void> sendGift(UUID targetPlayerUUID, ItemStack itemStack, String senderName, long expireSeconds);

    /**
     * 특정 플레이어의 선물 목록을 조회합니다.
     * 이 메서드는 비동기적으로 작동하며, 조회된 선물 목록을 CompletableFuture로 반환합니다.
     *
     * @param playerUUID 선물 목록을 조회할 플레이어의 UUID.
     * @param limit 조회할 최대 선물 개수.
     * @return 선물 목록을 포함하는 CompletableFuture.
     */
    CompletableFuture<List<Gift>> getPlayerGifts(UUID playerUUID, int limit);

    /**
     * 특정 플레이어의 만료되지 않은 선물 개수를 조회합니다.
     * 이 메서드는 비동기적으로 작동하며, 선물 개수를 CompletableFuture로 반환합니다.
     *
     * @param playerUUID 선물 개수를 조회할 플레이어의 UUID.
     * @return 선물 개수를 포함하는 CompletableFuture.
     */
    CompletableFuture<Integer> getPlayerGiftCount(UUID playerUUID);
}