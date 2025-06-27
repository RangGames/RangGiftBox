package rang.games.rangGiftBox.event;

import org.bukkit.event.Event;
import rang.games.rangGiftBox.model.Gift;

public abstract class GiftBoxEvent extends Event {
    protected final Gift gift;

    public GiftBoxEvent(Gift gift) {
        this.gift = gift;
    }

    /**
     * 이 이벤트와 관련된 선물 객체를 반환합니다.
     * @return 선물 객체 (Gift).
     */
    public Gift getGift() {
        return gift;
    }
}
