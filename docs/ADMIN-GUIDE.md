# RangGiftBox Administrator Guide

This guide provides detailed instructions for server administrators on how to install, configure, and manage RangGiftBox.

## Table of Contents

- [Installation](#installation)
- [Initial Setup](#initial-setup)
- [Configuration](#configuration)
- [Commands and Permissions](#commands-and-permissions)
- [Database Management](#database-management)
- [Common Use Cases](#common-use-cases)
- [Troubleshooting](#troubleshooting)
- [Performance Tuning](#performance-tuning)

## Installation

### Prerequisites

1. **Server Requirements:**
   - Spigot/Paper 1.21.4 or higher
   - Java 21 or higher
   - 50MB free disk space

2. **Database Requirements:**
   - MySQL 5.7+ or MariaDB 10.3+
   - Database user with CREATE, SELECT, INSERT, UPDATE, DELETE permissions
   - At least 100MB free database space (grows with usage)

### Step-by-Step Installation

1. **Download the Plugin**
   ```
   Download RangGiftBox-1.0.2-SNAPSHOT.jar from the releases page
   ```

2. **Install the Plugin**
   ```bash
   # Navigate to your server directory
   cd /path/to/your/server
   
   # Copy the JAR to plugins folder
   cp RangGiftBox-1.0.2-SNAPSHOT.jar plugins/
   ```

3. **First Start**
   ```bash
   # Start the server
   java -jar spigot.jar
   
   # The plugin will generate default configuration
   # and then disable itself due to missing database config
   ```

4. **Configure Database**
   - Edit `plugins/RangGiftBox/config.yml`
   - Set your database credentials (see Configuration section)

5. **Restart Server**
   ```bash
   # Restart to apply configuration
   /restart
   # or
   java -jar spigot.jar
   ```

## Initial Setup

### 1. Database Setup

Create a database for RangGiftBox:

```sql
-- Connect to MySQL/MariaDB
mysql -u root -p

-- Create database
CREATE DATABASE giftbox CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user (optional but recommended)
CREATE USER 'giftbox'@'localhost' IDENTIFIED BY 'secure_password';

-- Grant permissions
GRANT CREATE, SELECT, INSERT, UPDATE, DELETE ON giftbox.* TO 'giftbox'@'localhost';

-- Apply changes
FLUSH PRIVILEGES;
```

### 2. Basic Configuration

Edit `plugins/RangGiftBox/config.yml`:

```yaml
database:
  host: "localhost"
  port: 3306
  database: "giftbox"
  username: "giftbox"
  password: "secure_password"

# Check for expired gifts every 10 minutes
expiration-check-interval: 600

# Disable debug logging in production
debug: false
```

### 3. Verify Installation

Check the console for:
```
[RangGiftBox] Database tables initialized successfully
[RangGiftBox] RangGiftBox has been enabled successfully!
```

Test the plugin:
```
/giftbox
```

## Configuration

### Complete Configuration Reference

```yaml
# Database connection settings
database:
  host: "localhost"          # Database server address
  port: 3306                 # Database port (3306 for MySQL/MariaDB)
  database: "giftbox"        # Database name
  username: "giftbox"        # Database username
  password: "secure_password" # Database password

# How often to check for expired gifts (in seconds)
# Minimum: 60, Recommended: 600 (10 minutes)
expiration-check-interval: 600

# Enable debug logging (verbose output)
# Set to true only when troubleshooting
debug: false

# All user-facing messages
messages:
  # Prefix added to all messages
  prefix: "&f[&d선물함&f] "
  
  # Error messages
  no-permission: "&c이 명령어를 사용할 권한이 없습니다."
  player-not-found: "&c플레이어 &e%player%&c님을 찾을 수 없습니다."
  no-item-in-hand: "&c아이템을 보내려면 손에 아이템을 들고 있어야 합니다."
  invalid-command-usage: "&c잘못된 명령어 사용법입니다. 사용법: %usage%"
  invalid-number: "&c만료 시간은 유효한 숫자여야 합니다."
  inventory-full: "&c인벤토리가 가득 찼습니다. 선물을 받으려면 공간을 확보해주세요."
  concurrent-claim-error: "&c이미 수령을 처리 중입니다. 잠시 기다려주세요."
  gift-expired: "&c이 선물은 만료되어 수령할 수 없습니다."
  loading-error: "&c선물함을 불러오는 중 오류가 발생했습니다. 다시 시도해주세요."
  
  # Success messages
  gift-sent: "&a손에 든 아이템을 &e%player%&a님에게 성공적으로 보냈습니다."
  gift-claimed: "&a선물을 성공적으로 수령했습니다."
  all-gifts-claimed: "&a&e%amount%&a개의 선물을 성공적으로 수령했습니다."
  no-gifts-to-claim: "&c선물함에 수령할 수 있는 선물이 없습니다."
  
  # Notification messages
  join-notification: "&a&e%amount%&a개의 선물이 당신을 기다리고 있습니다! &e/우편함&a을 입력하여 확인하세요."
  
  # GUI related
  gui-title: "선물함 (페이지: %page%)"
  loading-item-name: "&7로딩 중..."
  claim-all-item-name: "&a[ 아이템 모두 받기 ]"
  claim-all-item-lore:
    - "&7클릭 시 수령 가능한 모든 선물을"
    - "&7한 번에 받습니다."
  gift-item-lore:
    - "&f"
    - "&7보낸 사람: &e%sender%"
    - "&7수량: &e%amount%"
    - "&7받은 날짜: &e%date%"
    - "&7만료 기한: &e%expire%"
  expire-never: "영구 보관"
  
  # GUI message cooldown (seconds)
  gui-message-cooldown: 0.2
```

### Message Customization

#### Color Codes
Use `&` for color codes:
- `&a` - Green
- `&c` - Red
- `&e` - Yellow
- `&f` - White
- `&d` - Pink
- `&7` - Gray

#### Placeholders
Available placeholders:
- `%player%` - Player name
- `%amount%` - Amount/count
- `%sender%` - Gift sender
- `%date%` - Date formatted
- `%expire%` - Expiration date
- `%usage%` - Command usage
- `%page%` - Page number

### Language Packs

To change to English, replace the messages section:

```yaml
messages:
  prefix: "&f[&dGiftBox&f] "
  no-permission: "&cYou don't have permission to use this command."
  player-not-found: "&cPlayer &e%player%&c not found."
  no-item-in-hand: "&cYou must hold an item to send it."
  # ... etc
```

## Commands and Permissions

### Command Reference

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/우편함` | Open gift box GUI | `giftbox.user` | Everyone |
| `/선물함` | Alias for /우편함 | `giftbox.user` | Everyone |
| `/giftbox` | Alias for /우편함 | `giftbox.user` | Everyone |
| `/우편함 지급 <player> <sender>` | Send gift without expiration | `giftbox.admin.give` | OP |
| `/우편함 지급 <player> <sender> <seconds>` | Send gift with expiration | `giftbox.admin.give.expire` | OP |

### Permission Setup

#### Using LuckPerms:
```bash
# Give user permission to open gift box
/lp user <player> permission set giftbox.user true

# Give admin all permissions
/lp group admin permission set giftbox.admin.* true

# Create gift sender role
/lp group create giftsender
/lp group giftsender permission set giftbox.admin.give true
```

#### Using PermissionsEx:
```bash
# User permission
/pex user <player> add giftbox.user

# Admin permissions
/pex group admin add giftbox.admin.*
```

### Custom Permission Groups

Create specialized roles:

```yaml
# Moderator - can send gifts but not set expiration
moderator:
  - giftbox.user
  - giftbox.admin.give

# Event Manager - full gift control
event-manager:
  - giftbox.user
  - giftbox.admin.give
  - giftbox.admin.give.expire

# Player - basic access only
player:
  - giftbox.user
```

## Database Management

### Backup Procedures

#### Automated Backup Script
```bash
#!/bin/bash
# backup-giftbox.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/path/to/backups"
DB_NAME="giftbox"
DB_USER="giftbox"
DB_PASS="secure_password"

# Create backup
mysqldump -u $DB_USER -p$DB_PASS $DB_NAME > $BACKUP_DIR/giftbox_$DATE.sql

# Compress
gzip $BACKUP_DIR/giftbox_$DATE.sql

# Keep only last 7 days
find $BACKUP_DIR -name "giftbox_*.sql.gz" -mtime +7 -delete
```

#### Manual Backup
```bash
# Backup database
mysqldump -u giftbox -p giftbox > giftbox_backup.sql

# Restore database
mysql -u giftbox -p giftbox < giftbox_backup.sql
```

### Database Maintenance

#### View Database Size
```sql
SELECT 
    table_name AS 'Table',
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.TABLES 
WHERE table_schema = 'giftbox';
```

#### Clean Old Logs
```sql
-- Delete logs older than 30 days
DELETE FROM present_log 
WHERE TimeStamp < UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL 30 DAY)) * 1000;

-- Optimize tables after cleanup
OPTIMIZE TABLE present_log;
```

#### Check Gift Statistics
```sql
-- Total gifts sent
SELECT COUNT(*) as total_gifts FROM present;

-- Gifts by sender
SELECT Sender, COUNT(*) as gift_count 
FROM present 
GROUP BY Sender 
ORDER BY gift_count DESC;

-- Average gifts per player
SELECT AVG(gift_count) as avg_gifts
FROM (
    SELECT UUID, COUNT(*) as gift_count 
    FROM present 
    GROUP BY UUID
) as player_gifts;
```

## Common Use Cases

### 1. Server-Wide Gift Distribution

Send gifts to all online players:
```
/우편함 지급 Player1 Server
/우편함 지급 Player2 Server
...
```

Or use console commands:
```
console command: gift-all
```

### 2. Event Rewards

For limited-time events:
```
# 24-hour expiration for event items
/우편함 지급 <player> "Halloween Event" 86400
```

### 3. Vote Rewards

Integrate with voting plugins:
```java
// In your vote listener
Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
    "우편함 지급 " + player.getName() + " VoteReward");
```

### 4. Automated Daily Rewards

Using a scheduler plugin:
```
# Run daily at midnight
0 0 * * * /우편함 지급 %player% "Daily Login" 172800
```

### 5. Purchase Deliveries

For shop systems:
```java
// After successful purchase
Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
    "우편함 지급 " + buyer + " Shop -1");
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Plugin Won't Start

**Symptoms:**
- Plugin disables itself immediately
- Error: "Failed to create database manager"

**Solutions:**
```bash
# Check MySQL is running
systemctl status mysql

# Test connection
mysql -h localhost -u giftbox -p

# Verify credentials in config.yml
```

#### 2. "Could not initialize database tables"

**Check permissions:**
```sql
SHOW GRANTS FOR 'giftbox'@'localhost';
-- Should include CREATE, SELECT, INSERT, UPDATE, DELETE
```

**Manual table creation:**
```sql
USE giftbox;

CREATE TABLE IF NOT EXISTS present (
    ID VARCHAR(36) PRIMARY KEY,
    UUID VARCHAR(36) NOT NULL,
    ItemStack TEXT NOT NULL,
    Count INT NOT NULL,
    Sender VARCHAR(255) NOT NULL,
    TimeStamp BIGINT NOT NULL,
    ExpireStamp BIGINT NOT NULL,
    INDEX uuid_index (UUID)
);

CREATE TABLE IF NOT EXISTS present_log (
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

#### 3. Lag When Opening Gift Box

**Enable debug mode:**
```yaml
debug: true
```

**Check for:**
- Database connection delays
- Large number of gifts per player
- Network latency to database

**Solutions:**
- Increase connection pool size
- Add database indexes
- Use local database

#### 4. Gifts Not Expiring

**Verify:**
```sql
-- Check expiration task
SELECT * FROM present 
WHERE ExpireStamp > 0 
AND ExpireStamp < UNIX_TIMESTAMP() * 1000;
```

**Fix:**
- Check server time is correct
- Verify expiration-check-interval
- Look for errors in console

#### 5. Players Can't Claim Gifts

**Common causes:**
- Full inventory
- Expired gifts
- Database connection issues

**Debug steps:**
1. Enable debug mode
2. Have player try `/giftbox`
3. Check console for errors
4. Verify gift exists in database

### Debug Mode

Enable comprehensive logging:
```yaml
debug: true
```

Debug output includes:
- Database connection attempts
- Query execution times
- Gift operation details
- Error stack traces

**Important:** Disable debug mode in production!

## Performance Tuning

### Database Optimization

#### 1. Connection Pool Settings

For high-traffic servers, adjust in the code:
```java
// DatabaseManager.java
config.setMaximumPoolSize(20);      // Increase from 10
config.setMinimumIdle(5);           // Increase from 2
config.setConnectionTimeout(15000);  // Decrease from 30000
```

#### 2. Query Optimization

Add indexes for better performance:
```sql
-- Add index for sender queries
ALTER TABLE present ADD INDEX sender_index (Sender);

-- Add composite index for expiration checks
ALTER TABLE present ADD INDEX expire_check (ExpireStamp, UUID);

-- Add index for log queries
ALTER TABLE present_log ADD INDEX player_time (PlayerUUID, TimeStamp);
```

#### 3. Batch Processing

For bulk operations:
```sql
-- Use transactions
START TRANSACTION;
-- Multiple inserts here
COMMIT;
```

### Server Configuration

#### 1. Expiration Check Interval

Balance between performance and accuracy:
```yaml
# For small servers (< 50 players)
expiration-check-interval: 300  # 5 minutes

# For medium servers (50-200 players)
expiration-check-interval: 600  # 10 minutes

# For large servers (200+ players)
expiration-check-interval: 1200  # 20 minutes
```

#### 2. GUI Performance

Limit gifts shown per page:
```java
// Currently hardcoded to 36
// Can be modified in GiftBoxGUI.java
```

### Monitoring

#### Performance Metrics

Monitor these values:
```sql
-- Average query time
SHOW GLOBAL STATUS LIKE 'Slow_queries';

-- Connection usage
SHOW PROCESSLIST;

-- Table sizes
SELECT table_name, table_rows, data_length, index_length
FROM information_schema.tables
WHERE table_schema = 'giftbox';
```

#### Server Impact

Use timings to monitor:
```
/timings on
# Wait 5 minutes
/timings paste
```

Look for:
- RangGiftBox event processing
- Database query times
- Task execution times

### Best Practices

1. **Regular Maintenance**
   - Clean old logs monthly
   - Optimize tables quarterly
   - Monitor database growth

2. **Backup Strategy**
   - Daily automated backups
   - Keep 7 days of backups
   - Test restore procedures

3. **Security**
   - Use strong database passwords
   - Limit database user permissions
   - Regular security updates

4. **Monitoring**
   - Set up alerts for database issues
   - Monitor gift accumulation
   - Track performance metrics

## Integration Examples

### With EssentialsX
```yaml
# In EssentialsX kit configuration
kits:
  starter:
    delay: 86400
    items:
      - diamond 5
    commands:
      - '/우편함 지급 {player} StarterKit'
```

### With Votifier
```java
@EventHandler
public void onVote(VotifierEvent event) {
    String player = event.getVote().getUsername();
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
        "우편함 지급 " + player + " VoteReward 604800");
}
```

### With Jobs Reborn
```yaml
# In Jobs configuration
Jobs:
  Miner:
    level-10-reward:
      commands:
        - '/우편함 지급 {player} "Jobs Level 10" -1'
```

## Support

For additional help:
1. Enable debug mode and check logs
2. Search existing issues on GitHub
3. Create detailed issue report with:
   - Server version
   - Plugin version
   - Error messages
   - Steps to reproduce