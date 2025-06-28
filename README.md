# RangGiftBox

## Overview

RangGiftBox is a comprehensive gift box system for Minecraft servers running on Spigot-based platforms. It allows server administrators and players to send items as gifts to other players, which can then be claimed through a user-friendly graphical interface. The plugin is designed with performance in mind, utilizing a database for storing gift data and asynchronous operations to prevent server lag.

## Features

  - **GUI-Based Gift Box**: Provides a clean and intuitive graphical interface for players to view and claim their gifts.
  - **Send Gifts via Command**: Admins can send the item they are holding to any player using a simple command.
  - **Database Storage**: All gift data is stored in a MySQL or MariaDB database, ensuring data persistence and reliability.
  - **Expiration System**: Gifts can be set to expire after a certain duration, or they can be permanent. Expired gifts are automatically cleaned up.
  - **Developer API**: A simple yet powerful API is available for other plugins to interact with the gift box system.
  - **Customizable Messages**: Nearly all messages sent to players can be customized through the configuration file, including prefixes and colors.
  - **Join Notifications**: Players are notified upon joining the server if they have pending gifts waiting for them.
  - **Logging**: Gift actions such as sending, claiming, and expiration are logged in the database for administrative purposes.
  - **Bulk Claim**: Players can claim all available gifts at once, provided they have enough inventory space.

## Commands

The main command for this plugin is `/우편함` (aliases: `/선물함`, `/giftbox`).

  - **`/우편함`**: Opens the gift box GUI for the player.
  - **`/우편함 지급 <player> <sender> [expiration_seconds]`**: Sends the item currently held in the sender's main hand to the specified player.
      - `<player>`: The name of the player to receive the gift.
      - `<sender>`: The name to be displayed as the sender.
      - `[expiration_seconds]` (Optional): The time in seconds until the gift expires. If not provided, the gift will never expire.

## Permissions

  - **`giftbox.user`**: Allows a player to open their gift box with the `/우편함` command. (Default: `true`)
  - **`giftbox.admin.give`**: Allows a user to use the `/우편함 지급` command. (Default: `op`)
  - **`giftbox.admin.give.expire`**: Allows a user to specify an expiration time when using the `/우편함 지급` command. (Default: `op`)

## Configuration

The plugin's behavior can be customized via the `config.yml` file.

```yaml
# Database connection settings
database:
  host: "localhost"
  port: 3306
  database: "giftbox"
  username: "root"
  password: "password"

# Interval in seconds for checking and removing expired gifts.
expiration-check-interval: 600

# Message configuration
messages:
  prefix: "&f[&d선물함&f] "
  no-permission: "&cYou do not have permission to use this command."
  gift-sent: "&aSuccessfully sent the item in your hand to &e%player%&a."
  player-not-found: "&cPlayer &e%player%&c could not be found."
  no-item-in-hand: "&cYou must be holding an item to send it."
  invalid-command-usage: "&cInvalid command usage. Usage: %usage%"
  invalid-number: "&cThe expiration time must be a valid number."
  inventory-full: "&cYour inventory is full. Please make space to receive gifts."
  gift-claimed: "&aYou have successfully claimed the gift."
  all-gifts-claimed: "&aSuccessfully claimed &e%amount%&a gifts. If your inventory was full, some items may not have been claimed."
  no-gifts-to-claim: "&cThere are no gifts to claim in your gift box."
  join-notification: "&a&e%amount%&a gifts are waiting for you! Type &e/우편함&a to check."
  gift-expired: "&cThis gift has expired and cannot be claimed."
  concurrent-claim-error: "&cClaim is already in progress. Please wait a moment."
  gui-title: "Gift Box (Page: %page%)"
  loading-item-name: "&7Loading..."
  claim-all-item-name: "&a[ Claim All Items ]"
  claim-all-item-lore:
    - "&7Click to receive all available gifts"
    - "&7at once."
  gift-item-lore:
    - "&f"
    - "&7From: &e%sender%"
    - "&7Amount: &e%amount%"
    - "&7Date Received: &e%date%"
    - "&7Expires: &e%expire%"
  expire-never: "Permanent"
  gui-message-cooldown: 0.2
```

## Developer API

RangGiftBox provides an API for other developers to integrate with. To use the API, you need to get the `GiftBoxAPI` instance from Bukkit's `ServicesManager`.

### Getting the API instance:

```java
import org.bukkit.plugin.RegisteredServiceProvider;
import rang.games.rangGiftBox.api.GiftBoxAPI;

// ...

RegisteredServiceProvider<GiftBoxAPI> provider = Bukkit.getServicesManager().getRegistration(GiftBoxAPI.class);
if (provider != null) {
    GiftBoxAPI api = provider.getProvider();
    // You can now use the API methods
}
```

### API Methods

The `GiftBoxAPI` interface provides the following methods:

  - **`CompletableFuture<Void> sendGift(UUID targetPlayerUUID, ItemStack itemStack, String senderName, long expireSeconds)`**: Sends an item to a player as a gift.
  - **`CompletableFuture<List<Gift>> getPlayerGifts(UUID playerUUID, int limit)`**: Retrieves a list of a player's gifts.
  - **`CompletableFuture<Integer> getPlayerGiftCount(UUID playerUUID)`**: Gets the number of unclaimed gifts for a player.

### Example Usage:

```java
// Example: Sending a Diamond to a player named "Notch" that expires in 1 day.
Player target = Bukkit.getPlayer("Notch");
if (target != null && api != null) {
    UUID targetUUID = target.getUniqueId();
    ItemStack giftItem = new ItemStack(Material.DIAMOND, 1);
    String sender = "Server";
    long expirationInSeconds = 86400; // 24 hours * 60 minutes * 60 seconds

    api.sendGift(targetUUID, giftItem, sender, expirationInSeconds).thenRun(() -> {
        System.out.println("Successfully sent a gift to " + target.getName());
    });
}
```

## Dependencies

This plugin requires the following:

  - **Spigot API**: Version 1.21 or higher.
  - **Java**: Version 21 or higher.
  - **Database**: MySQL or MariaDB.

The following libraries are shaded into the plugin JAR:

  - **HikariCP**: For high-performance database connection pooling.
  - **MariaDB JDBC Driver**: To connect to MariaDB/MySQL databases.
