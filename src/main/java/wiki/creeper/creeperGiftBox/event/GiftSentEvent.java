package wiki.creeper.creeperGiftBox.event;

import org.bukkit.event.HandlerList;
import wiki.creeper.creeperGiftBox.model.Gift;

public class GiftSentEvent extends GiftBoxEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public GiftSentEvent(Gift gift) {
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