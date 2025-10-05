package wiki.creeper.rangGiftBox.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import wiki.creeper.rangGiftBox.model.Gift;

public class GiftClaimedEvent extends GiftBoxEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    public GiftClaimedEvent(Gift gift, Player player) {
        super(gift);
        this.player = player;
    }

    /**
     * 선물을 수령한 플레이어를 반환합니다.
     * @return 선물을 수령한 플레이어 (Player).
     */
    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}