package rang.games.rangGiftBox.event;

import org.bukkit.event.HandlerList;
import rang.games.rangGiftBox.model.Gift;

public class GiftExpiredEvent extends GiftBoxEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public GiftExpiredEvent(Gift gift) {
        super(gift);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}