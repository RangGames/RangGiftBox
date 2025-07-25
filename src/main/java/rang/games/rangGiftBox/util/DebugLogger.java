package rang.games.rangGiftBox.util;

import rang.games.rangGiftBox.RangGiftBox;
import java.util.logging.Level;

/**
 * Utility class for debug logging
 */
public class DebugLogger {
    
    private final RangGiftBox plugin;
    private final boolean debugEnabled;
    
    public DebugLogger(RangGiftBox plugin) {
        this.plugin = plugin;
        this.debugEnabled = plugin.getConfig().getBoolean("debug", false);
    }
    
    public void debug(String message) {
        if (debugEnabled) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
    
    public void debug(String message, Object... args) {
        if (debugEnabled) {
            plugin.getLogger().info("[DEBUG] " + String.format(message, args));
        }
    }
    
    public void debugException(String message, Throwable throwable) {
        if (debugEnabled) {
            plugin.getLogger().log(Level.WARNING, "[DEBUG] " + message, throwable);
        }
    }
    
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
}