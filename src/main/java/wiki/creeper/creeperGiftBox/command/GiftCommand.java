package wiki.creeper.creeperGiftBox.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import wiki.creeper.creeperGiftBox.CreeperGiftBox;
import wiki.creeper.creeperGiftBox.config.ConfigManager;
import wiki.creeper.creeperGiftBox.database.DatabaseManager;
import wiki.creeper.creeperGiftBox.util.SchedulerUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Level;

public class GiftCommand implements CommandExecutor, TabCompleter {

    private final CreeperGiftBox plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;

    public GiftCommand(CreeperGiftBox plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(configManager.getMessage("invalid-command-usage", "%usage%", "/우편함 지급 <플레이어> <발신인> [만료시간]"));
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("giftbox.user")) {
                player.sendMessage(configManager.getMessage("no-permission"));
                return true;
            }
            plugin.getGiftBoxGUI().open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("지급")) {
            handleGiveCommand(sender, args);
            return true;
        }

        sender.sendMessage(configManager.getMessage("invalid-command-usage", "%usage%", "/우편함 [지급]"));
        return true;
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player) && args.length < 3) {
            sender.sendMessage(configManager.getMessage("invalid-command-usage", "%usage%", "/우편함 지급 <플레이어> <발신인> [만료시간]"));
            return;
        }

        if (args.length < 3 || args.length > 4) {
            sender.sendMessage(configManager.getMessage("invalid-command-usage", "%usage%", "/우편함 지급 <플레이어> <발신인> [만료시간]"));
            return;
        }

        if (!sender.hasPermission("giftbox.admin.give")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Player playerSender = (sender instanceof Player) ? (Player) sender : null;
        ItemStack itemInHand = (playerSender != null) ? playerSender.getInventory().getItemInMainHand() : null;

        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            sender.sendMessage(configManager.getMessage("no-item-in-hand"));
            return;
        }

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            sender.sendMessage(configManager.getMessage("player-not-found", "%player%", args[1]));
            return;
        }

        String from = args[2];
        long expireSeconds = -1;

        if (args.length == 4) {
            if (!sender.hasPermission("giftbox.admin.give.expire")) {
                sender.sendMessage(configManager.getMessage("no-permission"));
                return;
            }
            try {
                expireSeconds = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(configManager.getMessage("invalid-number"));
                return;
            }
        }

        plugin.getGiftBoxAPI().sendGift(
                targetPlayer.getUniqueId(),
                itemInHand.clone(),
                from,
                expireSeconds
        ).thenRunAsync(() -> {
            String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : targetPlayer.getUniqueId().toString();
            sender.sendMessage(configManager.getMessage("gift-sent", "%player%", targetName));
        }, SchedulerUtil.syncExecutor(plugin)).exceptionally(throwable -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to send gift via command", throwable);
            SchedulerUtil.runSync(plugin, () -> sender.sendMessage(
                    configManager.getRawMessage("prefix") + ChatColor.RED + "선물 지급에 실패했습니다. 콘솔 로그를 확인해주세요."
            ));
            return null;
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission("giftbox.admin.give")) {
                return Arrays.asList("지급");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("지급")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
