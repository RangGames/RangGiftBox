# RangGiftBox API Guide

This guide provides detailed information for developers who want to integrate with RangGiftBox.

## Table of Contents

- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Gift Model](#gift-model)
- [Events](#events)
- [Code Examples](#code-examples)
- [Best Practices](#best-practices)

## Getting Started

### Maven Dependency

First, add RangGiftBox to your project's dependencies:

```xml
<dependency>
    <groupId>rang.games</groupId>
    <artifactId>RangGiftBox</artifactId>
    <version>1.0.2-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

### Plugin Dependency

Add RangGiftBox as a dependency in your plugin.yml:

```yaml
depend: [RangGiftBox]
# or for soft dependency:
softdepend: [RangGiftBox]
```

### Accessing the API

```java
import rang.games.rangGiftBox.api.GiftBoxAPI;
import org.bukkit.Bukkit;

public class YourPlugin extends JavaPlugin {
    private GiftBoxAPI giftBoxAPI;
    
    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("RangGiftBox") != null) {
            giftBoxAPI = Bukkit.getServicesManager().load(GiftBoxAPI.class);
            if (giftBoxAPI == null) {
                getLogger().warning("RangGiftBox found but API not available!");
            }
        }
    }
}
```

## API Reference

### GiftBoxAPI Interface

All API methods return `CompletableFuture` for asynchronous operations.

#### sendGift()

Sends an item as a gift to a player.

```java
CompletableFuture<Void> sendGift(
    UUID targetPlayerUUID,
    ItemStack itemStack,
    String senderName,
    long expireSeconds
)
```

**Parameters:**
- `targetPlayerUUID` - The recipient's UUID (required, non-null)
- `itemStack` - The item to send (required, non-null, not AIR)
- `senderName` - The sender's name, max 100 characters (required, non-empty)
- `expireSeconds` - Expiration time in seconds, -1 for no expiration

**Returns:** CompletableFuture that completes when the gift is sent

**Throws:**
- `IllegalArgumentException` - If parameters are invalid
- `CompletionException` - If database operation fails

#### getPlayerGifts()

Retrieves a player's gift list.

```java
CompletableFuture<List<Gift>> getPlayerGifts(
    UUID playerUUID,
    int limit
)
```

**Parameters:**
- `playerUUID` - The player's UUID (required, non-null)
- `limit` - Maximum number of gifts to retrieve (1-100)

**Returns:** CompletableFuture containing the gift list

**Note:** Returns empty list on error instead of throwing exception

#### getPlayerGiftCount()

Gets the count of unclaimed gifts for a player.

```java
CompletableFuture<Integer> getPlayerGiftCount(UUID playerUUID)
```

**Parameters:**
- `playerUUID` - The player's UUID (required, non-null)

**Returns:** CompletableFuture containing the gift count (0 on error)

## Gift Model

The `Gift` class represents a gift in the system:

```java
public class Gift {
    private final String id;          // Unique gift ID (UUID)
    private final UUID playerUUID;    // Recipient's UUID
    private final ItemStack itemStack; // The gift item
    private final String sender;      // Sender's name
    private final long timestamp;     // When sent (milliseconds)
    private final long expireStamp;   // When expires (-1 = never)
    
    // Getters for all fields...
}
```

## Events

### GiftSentEvent

Fired when a gift is successfully sent.

```java
@EventHandler
public void onGiftSent(GiftSentEvent event) {
    Gift gift = event.getGift();
    String sender = gift.getSender();
    UUID recipient = gift.getPlayerUUID();
    ItemStack item = gift.getItemStack();
    
    getLogger().info(sender + " sent " + item.getType() + " to " + recipient);
}
```

### GiftClaimedEvent

Fired when a player claims a gift.

```java
@EventHandler
public void onGiftClaimed(GiftClaimedEvent event) {
    Gift gift = event.getGift();
    Player player = event.getPlayer();
    
    // Reward the player for claiming a gift
    player.sendMessage("You claimed a gift from " + gift.getSender() + "!");
}
```

### GiftExpiredEvent

Fired when a gift expires.

```java
@EventHandler
public void onGiftExpired(GiftExpiredEvent event) {
    Gift gift = event.getGift();
    
    // Log expired gifts
    getLogger().info("Gift " + gift.getId() + " expired");
}
```

## Code Examples

### Example 1: Sending a Welcome Gift

```java
public void sendWelcomeGift(Player player) {
    ItemStack welcomeKit = new ItemStack(Material.DIAMOND, 5);
    ItemMeta meta = welcomeKit.getItemMeta();
    meta.setDisplayName("§6Welcome Kit");
    welcomeKit.setItemMeta(meta);
    
    giftBoxAPI.sendGift(
        player.getUniqueId(),
        welcomeKit,
        "Server",
        -1  // Never expires
    ).thenRun(() -> {
        player.sendMessage("§aCheck your gift box for a welcome gift!");
    }).exceptionally(throwable -> {
        getLogger().severe("Failed to send welcome gift: " + throwable.getMessage());
        return null;
    });
}
```

### Example 2: Daily Rewards System

```java
public void sendDailyReward(Player player) {
    ItemStack reward = generateDailyReward();
    
    giftBoxAPI.sendGift(
        player.getUniqueId(),
        reward,
        "Daily Rewards",
        86400  // Expires in 24 hours
    ).thenRun(() -> {
        player.sendMessage("§aYour daily reward has been sent!");
    }).exceptionally(throwable -> {
        player.sendMessage("§cFailed to send daily reward. Please try again.");
        return null;
    });
}
```

### Example 3: Checking Gift Count

```java
public void checkGifts(Player player) {
    giftBoxAPI.getPlayerGiftCount(player.getUniqueId())
        .thenAccept(count -> {
            if (count > 0) {
                player.sendMessage("§eYou have §6" + count + " §egifts waiting!");
                player.sendMessage("§eUse §6/giftbox §eto claim them!");
            }
        });
}
```

### Example 4: Gift History Tracking

```java
private final Map<UUID, List<String>> giftHistory = new HashMap<>();

@EventHandler
public void onGiftClaimed(GiftClaimedEvent event) {
    Player player = event.getPlayer();
    Gift gift = event.getGift();
    
    giftHistory.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
        .add(gift.getSender() + " - " + gift.getItemStack().getType());
    
    // Achievement system
    if (giftHistory.get(player.getUniqueId()).size() >= 10) {
        player.sendMessage("§6Achievement Unlocked: Gift Collector!");
    }
}
```

### Example 5: Bulk Gift Sending

```java
public void sendBulkGifts(List<UUID> players, ItemStack reward, String sender) {
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    
    for (UUID playerUUID : players) {
        CompletableFuture<Void> future = giftBoxAPI.sendGift(
            playerUUID,
            reward.clone(),
            sender,
            604800  // 7 days
        );
        futures.add(future);
    }
    
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenRun(() -> {
            getLogger().info("Successfully sent gifts to " + players.size() + " players");
        })
        .exceptionally(throwable -> {
            getLogger().severe("Failed to send some gifts: " + throwable.getMessage());
            return null;
        });
}
```

## Best Practices

### 1. Always Handle Exceptions

The API uses CompletableFuture, so always handle exceptions:

```java
giftBoxAPI.sendGift(...).exceptionally(throwable -> {
    // Log the error
    getLogger().severe("Gift sending failed: " + throwable.getMessage());
    // Notify the player
    player.sendMessage("§cFailed to send gift. Please try again.");
    return null;
});
```

### 2. Clone ItemStacks

Always clone ItemStacks before sending to prevent external modification:

```java
ItemStack giftItem = originalItem.clone();
giftBoxAPI.sendGift(playerUUID, giftItem, sender, expireTime);
```

### 3. Validate Input

Although the API validates input, pre-validate for better user experience:

```java
if (itemStack == null || itemStack.getType() == Material.AIR) {
    player.sendMessage("§cYou cannot send an empty gift!");
    return;
}
```

### 4. Use Appropriate Expiration Times

Consider the nature of your gifts when setting expiration:

```java
// Event rewards - short expiration
long eventExpire = 172800;  // 2 days

// Purchased items - never expire
long purchasedExpire = -1;

// Daily rewards - moderate expiration
long dailyExpire = 604800;  // 7 days
```

### 5. Monitor Gift Count

Prevent gift box overflow by checking gift count:

```java
giftBoxAPI.getPlayerGiftCount(playerUUID).thenAccept(count -> {
    if (count >= 50) {
        player.sendMessage("§cYour gift box is almost full! Claim some gifts first.");
        return;
    }
    // Send the gift
});
```

### 6. Async Operations

Remember all API operations are asynchronous:

```java
// Wrong - This won't work
List<Gift> gifts = giftBoxAPI.getPlayerGifts(uuid, 10).get(); // Blocks thread!

// Correct
giftBoxAPI.getPlayerGifts(uuid, 10).thenAccept(gifts -> {
    // Process gifts here
});
```

### 7. Sender Name Guidelines

Use consistent and meaningful sender names:

```java
// Good sender names
"Server"
"Daily Rewards"
"Vote Reward"
"Quest: Dragon Slayer"
player.getName()

// Avoid
"System"  // Too generic
""        // Empty
"xxxxxxxxxx..."  // Too long (max 100 chars)
```

## Error Handling

### Common Errors and Solutions

1. **API Not Available**
```java
if (giftBoxAPI == null) {
    // Disable gift-related features
    getLogger().warning("GiftBox API not available - disabling gift features");
    return;
}
```

2. **Database Connection Issues**
```java
future.exceptionally(throwable -> {
    if (throwable.getMessage().contains("database")) {
        // Retry once after delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Retry the operation
        }, 100L); // 5 seconds
    }
    return null;
});
```

3. **Invalid Parameters**
```java
try {
    giftBoxAPI.sendGift(uuid, item, sender, expire);
} catch (IllegalArgumentException e) {
    player.sendMessage("§cInvalid gift parameters: " + e.getMessage());
}
```

## Performance Tips

1. **Batch Operations**: When sending multiple gifts, use parallel operations
2. **Cache Gift Counts**: Don't check gift count on every player action
3. **Limit API Calls**: Implement cooldowns for gift-related commands
4. **Use Appropriate Limits**: Don't request more gifts than needed

## Version Compatibility

This API guide is for RangGiftBox version 1.0.2. Always check for API changes when updating.

## Support

For API-related questions or issues:
1. Check the example code
2. Enable debug mode in config.yml
3. Check console logs for errors
4. Create an issue on GitHub with details