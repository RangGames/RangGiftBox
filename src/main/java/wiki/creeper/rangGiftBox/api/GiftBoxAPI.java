package wiki.creeper.rangGiftBox.api;

import org.bukkit.inventory.ItemStack;
import wiki.creeper.rangGiftBox.model.Gift;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * RangGiftBox Public API
 * 
 * This API provides thread-safe methods for interacting with the gift box system.
 * All methods return CompletableFuture for asynchronous operations.
 * 
 * @author RangGiftBox Team
 * @version 1.0
 */
public interface GiftBoxAPI {

    /**
     * 특정 플레이어에게 아이템을 선물로 보냅니다.
     * 이 메서드는 비동기적으로 작동하며, 선물 지급이 완료되면 CompletableFuture가 완료됩니다.
     *
     * @param targetPlayerUUID 선물을 받을 플레이어의 UUID. (null 불가)
     * @param itemStack 보낼 아이템 (ItemStack). AIR 타입이나 0개 이하의 아이템은 불가합니다. (null 불가)
     * @param senderName 발신인의 이름 (예: "Server", "Admin", 또는 플레이어 이름). 최대 100자로 제한됩니다. (null 불가, 빈 문자열 불가)
     * @param expireSeconds 만료 시간 (초 단위). -1은 영구 보관을 의미하며, 0 이상의 값은 해당 초 후에 만료됩니다.
     * @return 선물 지급 작업의 완료를 나타내는 CompletableFuture. 실패 시 예외를 포함합니다.
     * @throws IllegalArgumentException 잘못된 매개변수가 제공된 경우
     * @throws CompletionException 데이터베이스 작업 중 오류가 발생한 경우
     */
    CompletableFuture<Void> sendGift(UUID targetPlayerUUID, ItemStack itemStack, String senderName, long expireSeconds);

    /**
     * 특정 플레이어의 선물 목록을 조회합니다.
     * 이 메서드는 비동기적으로 작동하며, 조회된 선물 목록을 CompletableFuture로 반환합니다.
     * 만료된 선물은 자동으로 제외됩니다.
     *
     * @param playerUUID 선물 목록을 조회할 플레이어의 UUID. (null 불가)
     * @param limit 조회할 최대 선물 개수. 1 이상 100 이하의 값이어야 합니다.
     * @return 선물 목록을 포함하는 CompletableFuture. 오류 발생 시 빈 리스트를 반환합니다.
     * @throws IllegalArgumentException playerUUID가 null이거나 limit이 유효하지 않은 경우
     */
    CompletableFuture<List<Gift>> getPlayerGifts(UUID playerUUID, int limit);

    /**
     * 특정 플레이어의 만료되지 않은 선물 개수를 조회합니다.
     * 이 메서드는 비동기적으로 작동하며, 선물 개수를 CompletableFuture로 반환합니다.
     *
     * @param playerUUID 선물 개수를 조회할 플레이어의 UUID. (null 불가)
     * @return 선물 개수를 포함하는 CompletableFuture. 오류 발생 시 0을 반환합니다.
     * @throws IllegalArgumentException playerUUID가 null인 경우
     */
    CompletableFuture<Integer> getPlayerGiftCount(UUID playerUUID);
}