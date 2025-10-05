package wiki.creeper.rangGiftBox.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import wiki.creeper.rangGiftBox.RangGiftBox;
import wiki.creeper.rangGiftBox.config.ConfigManager;
import wiki.creeper.rangGiftBox.database.DatabaseManager;
import wiki.creeper.rangGiftBox.database.LogResult;
import wiki.creeper.rangGiftBox.event.GiftClaimedEvent;
import wiki.creeper.rangGiftBox.gui.GiftBoxGUI;
import wiki.creeper.rangGiftBox.model.Gift;
import wiki.creeper.rangGiftBox.util.SchedulerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GUIListener implements Listener {

    private final RangGiftBox plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final Map<UUID, Double> lastErrorMessageTime = new ConcurrentHashMap<>();

    private static final String METADATA_KEY = "GIFTBOX_ACTION";

    public GUIListener(RangGiftBox plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!event.getView().getTitle().startsWith(configManager.getRawMessage("gui-title").split("%")[0])) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        if (player.hasMetadata(METADATA_KEY)) {
            double currentTime = System.currentTimeMillis();
            double lastTime = lastErrorMessageTime.getOrDefault(player.getUniqueId(), 0D);
            double cooldown = configManager.getGuiMessageCooldown() * 1000D;

            if (currentTime - lastTime >= cooldown) {
                player.sendMessage(configManager.getMessage("concurrent-claim-error"));
                lastErrorMessageTime.put(player.getUniqueId(), currentTime);
            }
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(GiftBoxGUI.GIFT_ID_KEY, PersistentDataType.STRING)) {
            handleSingleClaim(player, container);
        } else if (container.has(GiftBoxGUI.GIFT_ACTION_KEY, PersistentDataType.STRING)) {
            String action = container.get(GiftBoxGUI.GIFT_ACTION_KEY, PersistentDataType.STRING);
            if (GiftBoxGUI.ACTION_CLAIM_ALL.equals(action)) {
                handleClaimAll(player);
            }
        }
    }

    private void handleSingleClaim(Player player, PersistentDataContainer container) {
        String giftId = container.get(GiftBoxGUI.GIFT_ID_KEY, PersistentDataType.STRING);
        if (giftId == null) return;

        player.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));

        databaseManager.getGifts(player.getUniqueId(), 100)
                .thenAcceptAsync(gifts -> {
                    try {
                        Gift targetGift = gifts.stream()
                                .filter(g -> g.getId().equals(giftId))
                                .findFirst()
                                .orElse(null);

                        if (targetGift == null) {
                            return;
                        }

                        if (targetGift.getExpireStamp() != -1 && System.currentTimeMillis() > targetGift.getExpireStamp()) {
                            player.sendMessage(configManager.getMessage("gift-expired"));
                            databaseManager.deleteGift(giftId).thenRun(() ->
                                    databaseManager.logAction(targetGift, LogResult.EXPIRED)
                            ).exceptionally(ex -> {
                                plugin.getLogger().log(Level.SEVERE, "Failed to delete expired gift", ex);
                                return null;
                            });
                            return;
                        }

                        if (player.getInventory().firstEmpty() == -1) {
                            player.sendMessage(configManager.getMessage("inventory-full"));
                            return;
                        }

                        try {
                            ItemStack originalItem = targetGift.getItemStack().clone();
                            player.getInventory().addItem(originalItem);
                            player.sendMessage(configManager.getMessage("gift-claimed"));

                            databaseManager.deleteGift(giftId).thenAccept(deleted -> {
                                if (deleted) {
                                    databaseManager.logAction(targetGift, LogResult.CLAIMED);
                                    SchedulerUtil.runSync(plugin, () ->
                                            Bukkit.getPluginManager().callEvent(new GiftClaimedEvent(targetGift, player))
                                    );
                                }
                            }).exceptionally(ex -> {
                                plugin.getLogger().log(Level.SEVERE, "Failed to delete claimed gift", ex);
                                return null;
                            });

                        } catch (Exception e) {
                            plugin.getLogger().log(Level.SEVERE, "Error claiming single gift", e);
                        }
                    } finally {
                        player.removeMetadata(METADATA_KEY, plugin);
                        if (player.isOnline()) {
                            plugin.getGiftBoxGUI().open(player);
                        }
                    }
                }, SchedulerUtil.syncExecutor(plugin))
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to process single gift claim", ex);
                    SchedulerUtil.runSync(plugin, () -> {
                        player.removeMetadata(METADATA_KEY, plugin);
                        if (player.isOnline()) {
                            plugin.getGiftBoxGUI().open(player);
                        }
                    });
                    return null;
                });
    }

    private void handleClaimAll(Player player) {
        player.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));

        databaseManager.getGifts(player.getUniqueId(), 36)
                .thenAcceptAsync(gifts -> {
                    try {
                        if (gifts.isEmpty()) {
                            player.sendMessage(configManager.getMessage("no-gifts-to-claim"));
                            return;
                        }

                        List<Gift> claimedGifts = new ArrayList<>();
                        List<String> claimedGiftIds = new ArrayList<>();
                        int claimedCount = 0;

                        for (Gift gift : gifts) {
                            if (gift.getExpireStamp() != -1 && System.currentTimeMillis() > gift.getExpireStamp()) {
                                databaseManager.deleteGift(gift.getId()).thenRun(() ->
                                        databaseManager.logAction(gift, LogResult.EXPIRED)
                                ).exceptionally(ex -> {
                                    plugin.getLogger().log(Level.WARNING,
                                            "Failed to delete expired gift during claim all", ex);
                                    return null;
                                });
                                continue;
                            }

                            if (player.getInventory().firstEmpty() == -1) {
                                player.sendMessage(configManager.getMessage("inventory-full"));
                                break;
                            }

                            try {
                                ItemStack originalItem = gift.getItemStack().clone();
                                player.getInventory().addItem(originalItem);
                                claimedGifts.add(gift);
                                claimedGiftIds.add(gift.getId());
                                claimedCount++;
                            } catch (Exception e) {
                                plugin.getLogger().log(Level.SEVERE, "Error claiming multiple gifts", e);
                            }
                        }

                        if (!claimedGiftIds.isEmpty()) {
                            final int finalClaimedCount = claimedCount;
                            databaseManager.deleteGifts(claimedGiftIds)
                                    .thenAcceptAsync(deletedCount -> {
                                        claimedGifts.forEach(g -> {
                                            databaseManager.logAction(g, LogResult.CLAIMED);
                                            SchedulerUtil.runSync(plugin, () ->
                                                    Bukkit.getPluginManager().callEvent(new GiftClaimedEvent(g, player))
                                            );
                                        });
                                        player.sendMessage(configManager.getMessage("all-gifts-claimed", "%amount%", String.valueOf(finalClaimedCount)));
                                    }, SchedulerUtil.syncExecutor(plugin))
                                    .exceptionally(ex -> {
                                        plugin.getLogger().log(Level.SEVERE, "Failed to delete claimed gifts", ex);
                                        SchedulerUtil.runSync(plugin, () ->
                                                player.sendMessage(configManager.getMessage("all-gifts-claimed", "%amount%", String.valueOf(finalClaimedCount))));
                                        return null;
                                    });
                        } else if (claimedCount == 0 && !gifts.isEmpty()) {
                            if (player.getInventory().firstEmpty() == -1) {
                                player.sendMessage(configManager.getMessage("inventory-full"));
                            }
                        }
                    } finally {
                        player.removeMetadata(METADATA_KEY, plugin);
                        if (player.isOnline()) {
                            plugin.getGiftBoxGUI().open(player);
                        }
                    }
                }, SchedulerUtil.syncExecutor(plugin))
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to claim all gifts", ex);
                    SchedulerUtil.runSync(plugin, () -> {
                        player.removeMetadata(METADATA_KEY, plugin);
                        if (player.isOnline()) {
                            plugin.getGiftBoxGUI().open(player);
                        }
                    });
                    return null;
                });
    }
}
