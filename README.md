# RangGiftBox

A comprehensive gift box system for Minecraft Spigot servers that allows players to send and receive items as gifts with database persistence and expiration support.

## Features

- **Gift System**: Send items to other players as gifts
- **Database Persistence**: All gifts are stored in MySQL/MariaDB
- **Expiration Support**: Set expiration times for gifts
- **GUI Interface**: User-friendly chest GUI for managing gifts
- **Async Operations**: All database operations are asynchronous for optimal performance
- **Public API**: Developer-friendly API for integration with other plugins
- **Event System**: Custom events for gift lifecycle (sent, claimed, expired)
- **Comprehensive Logging**: Built-in debug mode and detailed error logging

## Requirements

- Minecraft Server: Spigot 1.21.4 or higher
- Java: 21 or higher
- Database: MySQL 5.7+ or MariaDB 10.3+

## Installation

1. Download the latest `RangGiftBox-1.0.2-SNAPSHOT.jar` from releases
2. Place the JAR file in your server's `plugins` folder
3. Start the server to generate the default configuration
4. Edit `plugins/RangGiftBox/config.yml` with your database credentials
5. Restart the server or reload the plugin

## Configuration

### Database Configuration
```yaml
database:
  host: "localhost"
  port: 3306
  database: "giftbox"
  username: "root"
  password: "password"
```

### Other Settings
```yaml
# How often to check for expired gifts (in seconds)
expiration-check-interval: 600

# Enable debug logging
debug: false

# GUI message cooldown (in seconds)
messages:
  gui-message-cooldown: 0.2
```

## Commands

### Player Commands
- `/우편함` (aliases: `/선물함`, `/giftbox`) - Opens the gift box GUI

### Admin Commands
- `/우편함 지급 <player> <sender> [expire_seconds]` - Send the item in hand as a gift
  - `<player>`: Target player name
  - `<sender>`: Sender name (e.g., "Server", "Admin", or player name)
  - `[expire_seconds]`: Optional expiration time in seconds (-1 for never expire)

## Permissions

- `giftbox.user` - Allows opening the gift box GUI (default: true)
- `giftbox.admin.give` - Allows sending gifts to players (default: op)
- `giftbox.admin.give.expire` - Allows setting expiration time on gifts (default: op)

## API Usage

RangGiftBox provides a public API for developers to integrate with their plugins.

### Getting the API
```java
GiftBoxAPI api = Bukkit.getServicesManager().load(GiftBoxAPI.class);
```

### Sending a Gift
```java
api.sendGift(
    playerUUID,           // Target player's UUID
    itemStack,            // ItemStack to send
    "Server",            // Sender name
    3600                 // Expire in 1 hour (use -1 for no expiration)
).thenRun(() -> {
    // Gift sent successfully
}).exceptionally(throwable -> {
    // Handle error
    return null;
});
```

### Getting Player's Gifts
```java
api.getPlayerGifts(playerUUID, 36).thenAccept(gifts -> {
    for (Gift gift : gifts) {
        // Process each gift
    }
});
```

### Getting Gift Count
```java
api.getPlayerGiftCount(playerUUID).thenAccept(count -> {
    player.sendMessage("You have " + count + " gifts!");
});
```

## Events

RangGiftBox fires the following custom events:

- `GiftSentEvent` - Fired when a gift is sent
- `GiftClaimedEvent` - Fired when a gift is claimed
- `GiftExpiredEvent` - Fired when a gift expires

### Event Example
```java
@EventHandler
public void onGiftClaimed(GiftClaimedEvent event) {
    Gift gift = event.getGift();
    Player player = event.getPlayer();
    // Handle the event
}
```

## Database Schema

### present (Active Gifts)
```sql
CREATE TABLE present (
    ID VARCHAR(36) PRIMARY KEY,
    UUID VARCHAR(36) NOT NULL,
    ItemStack TEXT NOT NULL,
    Count INT NOT NULL,
    Sender VARCHAR(255) NOT NULL,
    TimeStamp BIGINT NOT NULL,
    ExpireStamp BIGINT NOT NULL,
    INDEX uuid_index (UUID)
);
```

### present_log (Gift History)
```sql
CREATE TABLE present_log (
    LogID INT AUTO_INCREMENT PRIMARY KEY,
    GiftID VARCHAR(36) NOT NULL,
    PlayerUUID VARCHAR(36) NOT NULL,
    ItemStack TEXT NOT NULL,
    Count INT NOT NULL,
    Sender VARCHAR(255) NOT NULL,
    Result INT NOT NULL,
    TimeStamp BIGINT NOT NULL
);
```

## Building from Source

This project uses Maven for dependency management.

```bash
# Clone the repository
git clone https://github.com/yourusername/RangGiftBox.git

# Navigate to the project directory
cd RangGiftBox

# Build the project
mvn clean package
```

The compiled JAR will be in the `target` directory.

## Performance Considerations

- All database operations are asynchronous to prevent server lag
- Connection pooling is implemented using HikariCP
- Batch operations are used for bulk deletes
- GUI operations include spam protection
- Expired gifts are cleaned up periodically

## Troubleshooting

### Common Issues

1. **"Could not initialize database tables!"**
   - Check your database credentials in config.yml
   - Ensure the database server is running
   - Verify the user has CREATE TABLE permissions

2. **"Failed to load gifts" error in GUI**
   - Check database connectivity
   - Look for errors in the console log
   - Enable debug mode for more detailed logging

3. **Gifts not expiring**
   - Check the expiration-check-interval setting
   - Verify the server time is correct
   - Check for errors in the ExpirationTask

### Debug Mode

Enable debug mode in config.yml for detailed logging:
```yaml
debug: true
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues, feature requests, or questions, please create an issue on the GitHub repository.