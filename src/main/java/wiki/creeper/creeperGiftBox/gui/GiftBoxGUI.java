package wiki.creeper.creeperGiftBox.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import wiki.creeper.creeperGiftBox.CreeperGiftBox;
import wiki.creeper.creeperGiftBox.config.ConfigManager;
import wiki.creeper.creeperGiftBox.database.DatabaseManager;
import wiki.creeper.creeperGiftBox.model.Gift;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class GiftBoxGUI {

    private final CreeperGiftBox plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd. HH:mm");
    private final Map<UUID, Long> lastOpenTime = new ConcurrentHashMap<>();
    private static final long OPEN_COOLDOWN = 500; // 500ms cooldown between opens

    public static final NamespacedKey GIFT_ID_KEY = new NamespacedKey(CreeperGiftBox.getPlugin(CreeperGiftBox.class), "gift_id");
    public static final NamespacedKey GIFT_ACTION_KEY = new NamespacedKey(CreeperGiftBox.getPlugin(CreeperGiftBox.class), "gift_action");
    public static final String ACTION_CLAIM_ALL = "claim_all";

    public GiftBoxGUI(CreeperGiftBox plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.configManager = plugin.getConfigManager();
    }

    public void open(Player player) {
        // Prevent spam opening
        long currentTime = System.currentTimeMillis();
        Long lastOpen = lastOpenTime.get(player.getUniqueId());
        if (lastOpen != null && currentTime - lastOpen < OPEN_COOLDOWN) {
            return;
        }
        lastOpenTime.put(player.getUniqueId(), currentTime);
        
        int page = 1;
        String title = configManager.getRawMessage("gui-title").replace("%page%", String.valueOf(page));
        Inventory gui = Bukkit.createInventory(null, 54, title);

        // Show loading indicator
        ItemStack loadingItem = createLoadingItem();
        gui.setItem(4, loadingItem);

        player.openInventory(gui);

        // Load gifts asynchronously
        databaseManager.getGifts(player.getUniqueId(), 36).thenAccept(gifts -> {
            // Only update if player still has the GUI open
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.getOpenInventory() != null && 
                    player.getOpenInventory().getTitle().equals(title)) {
                    populateGUI(gui, gifts, player);
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to load gifts for player " + player.getName() + ": " + throwable.getMessage());
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                player.sendMessage(configManager.getMessage("loading-error", "Failed to load gifts. Please try again."));
            });
            return null;
        });
    }
    
    private ItemStack createLoadingItem() {
        ItemStack loadingItem = new ItemStack(Material.PAPER);
        ItemMeta loadingMeta = loadingItem.getItemMeta();
        if (loadingMeta != null) {
            loadingMeta.setDisplayName(configManager.getRawMessage("loading-item-name"));
            loadingItem.setItemMeta(loadingMeta);
        }
        return loadingItem;
    }

    private void populateGUI(Inventory gui, List<Gift> gifts, Player player) {
        gui.clear();

        // Only show claim all button if there are gifts
        if (!gifts.isEmpty()) {
            ItemStack claimAllItem = createClaimAllItem();
            gui.setItem(4, claimAllItem);
        }

        // Populate gifts efficiently
        int slot = 9;
        for (Gift gift : gifts) {
            if (slot >= 45) break; // Leave bottom row empty
            
            ItemStack displayItem = createGiftDisplayItem(gift);
            if (displayItem != null) {
                gui.setItem(slot, displayItem);
            }
            slot++;
        }
        
        // Clean up old entries from lastOpenTime map periodically
        if (lastOpenTime.size() > 100) {
            long cutoffTime = System.currentTimeMillis() - 60000; // 1 minute ago
            lastOpenTime.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
        }
    }
    
    private ItemStack createClaimAllItem() {
        ItemStack claimAllItem = new ItemStack(Material.CHEST_MINECART);
        ItemMeta claimAllMeta = claimAllItem.getItemMeta();
        if (claimAllMeta != null) {
            claimAllMeta.setDisplayName(configManager.getRawMessage("claim-all-item-name"));
            claimAllMeta.setLore(configManager.getMessageList("claim-all-item-lore"));
            claimAllMeta.getPersistentDataContainer().set(GIFT_ACTION_KEY, PersistentDataType.STRING, ACTION_CLAIM_ALL);
            claimAllItem.setItemMeta(claimAllMeta);
        }
        return claimAllItem;
    }
    
    private ItemStack createGiftDisplayItem(Gift gift) {
        try {
            ItemStack displayItem = gift.getItemStack().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                // Format dates once
                String dateStr = dateFormat.format(new Date(gift.getTimestamp()));
                String expireStr = gift.getExpireStamp() == -1 
                    ? configManager.getRawMessage("expire-never") 
                    : dateFormat.format(new Date(gift.getExpireStamp()));
                
                List<String> lore = configManager.getMessageList("gift-item-lore",
                        "%sender%", gift.getSender(),
                        "%amount%", String.valueOf(gift.getItemStack().getAmount()),
                        "%date%", dateStr,
                        "%expire%", expireStr
                );
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(GIFT_ID_KEY, PersistentDataType.STRING, gift.getId());
                displayItem.setItemMeta(meta);
            }
            return displayItem;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create display item for gift " + gift.getId() + ": " + e.getMessage());
            return null;
        }
    }
}