package wiki.creeper.creeperGiftBox.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Gift {
    private final String id;
    private final UUID playerUUID;
    private final ItemStack itemStack;
    private final String sender;
    private final long timestamp;
    private final long expireStamp;

    public Gift(String id, UUID playerUUID, ItemStack itemStack, String sender, long timestamp, long expireStamp) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.itemStack = itemStack;
        this.sender = sender;
        this.timestamp = timestamp;
        this.expireStamp = expireStamp;
    }

    public String getId() {
        return id;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public String getSender() {
        return sender;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getExpireStamp() {
        return expireStamp;
    }
}