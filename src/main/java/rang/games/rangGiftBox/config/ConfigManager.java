package rang.games.rangGiftBox.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import rang.games.rangGiftBox.RangGiftBox;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {

    private final RangGiftBox plugin;
    private FileConfiguration config;

    public ConfigManager(RangGiftBox plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        this.config = this.plugin.getConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getDbHost() {
        return config.getString("database.host", "localhost");
    }

    public int getDbPort() {
        return config.getInt("database.port", 3306);
    }

    public String getDbName() {
        return config.getString("database.database", "giftbox");
    }

    public String getDbUser() {
        return config.getString("database.username", "root");
    }

    public String getDbPassword() {
        return config.getString("database.password", "password");
    }

    public long getExpirationCheckInterval() {
        return config.getLong("expiration-check-interval", 600);
    }

    public double getGuiMessageCooldown() {
        return config.getDouble("messages.gui-message-cooldown", 0.2);
    }

    public String getMessage(String path, String... replacements) {
        String message = config.getString("messages." + path, "&cMessage not found: " + path);
        message = ChatColor.translateAlternateColorCodes('&', message);

        if (replacements.length > 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    message = message.replace(replacements[i], replacements[i + 1]);
                }
            }
        }
        return getRawMessage("prefix") + message;
    }

    public String getRawMessage(String path) {
        return ChatColor.translateAlternateColorCodes('&', config.getString("messages." + path, ""));
    }

    public List<String> getMessageList(String path, String... replacements) {
        List<String> messages = config.getStringList("messages." + path);
        return messages.stream().map(line -> {
            String processedLine = ChatColor.translateAlternateColorCodes('&', line);
            if (replacements.length > 0) {
                for (int i = 0; i < replacements.length; i += 2) {
                    if (i + 1 < replacements.length) {
                        processedLine = processedLine.replace(replacements[i], replacements[i + 1]);
                    }
                }
            }
            return processedLine;
        }).collect(Collectors.toList());
    }
}