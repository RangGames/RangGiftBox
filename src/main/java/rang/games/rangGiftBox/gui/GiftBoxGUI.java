package rang.games.rangGiftBox.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import rang.games.rangGiftBox.RangGiftBox;
import rang.games.rangGiftBox.config.ConfigManager;
import rang.games.rangGiftBox.database.DatabaseManager;
import rang.games.rangGiftBox.model.Gift;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class GiftBoxGUI {

    private final RangGiftBox plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd. HH:mm");

    public static final NamespacedKey GIFT_ID_KEY = new NamespacedKey(RangGiftBox.getPlugin(RangGiftBox.class), "gift_id");
    public static final NamespacedKey GIFT_ACTION_KEY = new NamespacedKey(RangGiftBox.getPlugin(RangGiftBox.class), "gift_action");
    public static final String ACTION_CLAIM_ALL = "claim_all";

    public GiftBoxGUI(RangGiftBox plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.configManager = plugin.getConfigManager();
    }

    public void open(Player player) {
        int page = 1;
        Inventory gui = Bukkit.createInventory(null, 54, configManager.getRawMessage("gui-title").replace("%page%", String.valueOf(page)));

        ItemStack loadingItem = new ItemStack(Material.PAPER);
        ItemMeta loadingMeta = loadingItem.getItemMeta();
        loadingMeta.setDisplayName(configManager.getRawMessage("loading-item-name"));
        loadingItem.setItemMeta(loadingMeta);
        gui.setItem(4, loadingItem);

        player.openInventory(gui);

        databaseManager.getGifts(player.getUniqueId(), 36).thenAccept(gifts -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.getOpenInventory().getTitle().equals(configManager.getRawMessage("gui-title").replace("%page%", String.valueOf(page)))) {
                    populateGUI(gui, gifts);
                }
            });
        });
    }

    private void populateGUI(Inventory gui, List<Gift> gifts) {
        gui.clear();

        ItemStack claimAllItem = new ItemStack(Material.CHEST_MINECART);
        ItemMeta claimAllMeta = claimAllItem.getItemMeta();
        claimAllMeta.setDisplayName(configManager.getRawMessage("claim-all-item-name"));
        claimAllMeta.setLore(configManager.getMessageList("claim-all-item-lore"));
        claimAllMeta.getPersistentDataContainer().set(GIFT_ACTION_KEY, PersistentDataType.STRING, ACTION_CLAIM_ALL);
        claimAllItem.setItemMeta(claimAllMeta);
        gui.setItem(4, claimAllItem);

        int slot = 9;
        for (Gift gift : gifts) {
            if (slot >= 45) break;
            ItemStack displayItem = gift.getItemStack().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = configManager.getMessageList("gift-item-lore",
                        "%sender%", gift.getSender(),
                        "%amount%", String.valueOf(gift.getItemStack().getAmount()),
                        "%date%", dateFormat.format(new Date(gift.getTimestamp())),
                        "%expire%", gift.getExpireStamp() == -1 ? configManager.getRawMessage("expire-never") : dateFormat.format(new Date(gift.getExpireStamp()))
                );
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(GIFT_ID_KEY, PersistentDataType.STRING, gift.getId());
                displayItem.setItemMeta(meta);
            }
            gui.setItem(slot++, displayItem);
        }
    }
}