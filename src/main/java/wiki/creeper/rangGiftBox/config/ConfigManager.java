package wiki.creeper.rangGiftBox.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import wiki.creeper.rangGiftBox.RangGiftBox;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {

    private final RangGiftBox plugin;
    private FileConfiguration config;

    public ConfigManager(RangGiftBox plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        this.config = this.plugin.getConfig();
        validateConfig();
    }
    
    private void validateConfig() {
        boolean hasErrors = false;
        
        // Validate database configuration
        if (getDbHost().isEmpty()) {
            plugin.getLogger().severe("Database host is not configured!");
            hasErrors = true;
        }
        
        if (getDbPort() < 1 || getDbPort() > 65535) {
            plugin.getLogger().severe("Database port is invalid! Must be between 1 and 65535.");
            hasErrors = true;
        }
        
        if (getDbName().isEmpty()) {
            plugin.getLogger().severe("Database name is not configured!");
            hasErrors = true;
        }
        
        if (getDbUser().isEmpty()) {
            plugin.getLogger().severe("Database username is not configured!");
            hasErrors = true;
        }
        
        // Validate expiration check interval
        if (getExpirationCheckInterval() < 60) {
            plugin.getLogger().warning("Expiration check interval is very low (< 60 seconds). This may impact performance.");
        }
        
        // Validate GUI message cooldown
        if (getGuiMessageCooldown() < 0) {
            plugin.getLogger().warning("GUI message cooldown is negative. Setting to 0.2.");
            config.set("messages.gui-message-cooldown", 0.2);
        }
        
        // Check for required messages
        String[] requiredMessages = {
            "prefix", "no-permission", "gift-sent", "player-not-found",
            "no-item-in-hand", "invalid-command-usage", "invalid-number",
            "inventory-full", "gift-claimed", "all-gifts-claimed",
            "no-gifts-to-claim", "join-notification", "gift-expired",
            "concurrent-claim-error", "gui-title", "loading-item-name",
            "claim-all-item-name", "expire-never"
        };
        
        for (String key : requiredMessages) {
            if (!config.contains("messages." + key)) {
                plugin.getLogger().warning("Missing required message key: " + key);
                setDefaultMessage(key);
            }
        }
        
        if (hasErrors) {
            plugin.getLogger().severe("Critical configuration errors found! Please check your config.yml");
        }
    }
    
    private void setDefaultMessage(String key) {
        // Set default messages for missing keys
        switch (key) {
            case "prefix":
                config.set("messages.prefix", "&f[&dGiftBox&f] ");
                break;
            case "no-permission":
                config.set("messages.no-permission", "&cYou don't have permission to use this command.");
                break;
            case "loading-error":
                config.set("messages.loading-error", "&cFailed to load gifts. Please try again.");
                break;
            // Add more defaults as needed
        }
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        validateConfig();
        plugin.getLogger().info("Configuration reloaded and validated.");
    }

    public String getDbHost() {
        return config.getString("database.host", "localhost").trim();
    }

    public int getDbPort() {
        int port = config.getInt("database.port", 3306);
        // Ensure valid port range
        return (port >= 1 && port <= 65535) ? port : 3306;
    }

    public String getDbName() {
        return config.getString("database.database", "giftbox").trim();
    }

    public String getDbUser() {
        return config.getString("database.username", "root").trim();
    }

    public String getDbPassword() {
        // Password can be empty, so we don't trim
        return config.getString("database.password", "password");
    }

    public long getExpirationCheckInterval() {
        long interval = config.getLong("expiration-check-interval", 600);
        // Minimum 60 seconds to prevent performance issues
        return Math.max(interval, 60);
    }

    public double getGuiMessageCooldown() {
        double cooldown = config.getDouble("messages.gui-message-cooldown", 0.2);
        // Ensure non-negative cooldown
        return Math.max(cooldown, 0.0);
    }

    public String getMessage(String path, String... replacements) {
        String message = config.getString("messages." + path);
        if (message == null) {
            plugin.getLogger().warning("Missing message for path: " + path);
            message = "&cMessage not found: " + path;
        }
        
        message = ChatColor.translateAlternateColorCodes('&', message);

        // Apply replacements in pairs
        if (replacements.length > 0) {
            if (replacements.length % 2 != 0) {
                plugin.getLogger().warning("Odd number of replacements for message: " + path);
            }
            for (int i = 0; i < replacements.length - 1; i += 2) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        
        // Don't add prefix if the message is the prefix itself
        if (!"prefix".equals(path)) {
            return getRawMessage("prefix") + message;
        }
        return message;
    }

    public String getRawMessage(String path) {
        String message = config.getString("messages." + path, "");
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public List<String> getMessageList(String path, String... replacements) {
        List<String> messages = config.getStringList("messages." + path);
        if (messages.isEmpty()) {
            plugin.getLogger().warning("Empty or missing message list for path: " + path);
        }
        
        return messages.stream().map(line -> {
            String processedLine = ChatColor.translateAlternateColorCodes('&', line);
            // Apply replacements
            if (replacements.length > 0) {
                if (replacements.length % 2 != 0) {
                    plugin.getLogger().warning("Odd number of replacements for message list: " + path);
                }
                for (int i = 0; i < replacements.length - 1; i += 2) {
                    processedLine = processedLine.replace(replacements[i], replacements[i + 1]);
                }
            }
            return processedLine;
        }).collect(Collectors.toList());
    }
}