package com.Lino.lifesteal;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;

public class LifestealCommand implements CommandExecutor {

    private final Lifesteal plugin;

    public LifestealCommand(Lifesteal plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelp(sender);
                break;

            case "reload":
                if (!sender.hasPermission("lifesteal.reload")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to reload the plugin!");
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(ChatColor.GREEN + "Plugin successfully reloaded!");
                break;

            case "set":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to modify hearts!");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /lifesteal set <player> <hearts>");
                    return true;
                }
                handleSetHearts(sender, args[1], args[2]);
                break;

            case "get":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /lifesteal get <player>");
                    return true;
                }
                handleGetHearts(sender, args[1]);
                break;

            case "eliminate":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to eliminate players!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /lifesteal eliminate <player>");
                    return true;
                }
                handleEliminate(sender, args[1]);
                break;

            case "revive":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to revive players!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /lifesteal revive <player>");
                    return true;
                }
                handleRevive(sender, args[1]);
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== Lifesteal Help ==========");
        sender.sendMessage(ChatColor.YELLOW + "/lifesteal help" + ChatColor.WHITE + " - Display this menu");
        sender.sendMessage(ChatColor.YELLOW + "/lifesteal get <player>" + ChatColor.WHITE + " - Show a player's hearts");

        if (sender.hasPermission("lifesteal.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/lifesteal set <player> <hearts>" + ChatColor.WHITE + " - Set a player's hearts");
            sender.sendMessage(ChatColor.YELLOW + "/lifesteal eliminate <player>" + ChatColor.WHITE + " - Eliminate a player");
            sender.sendMessage(ChatColor.YELLOW + "/lifesteal revive <player>" + ChatColor.WHITE + " - Revive a player");
        }

        if (sender.hasPermission("lifesteal.reload")) {
            sender.sendMessage(ChatColor.YELLOW + "/lifesteal reload" + ChatColor.WHITE + " - Reload the configuration");
        }

        sender.sendMessage(ChatColor.GOLD + "==================================");
    }

    @SuppressWarnings("deprecation")
    private void handleSetHearts(CommandSender sender, String targetName, String heartsStr) {
        Player target = Bukkit.getPlayer(targetName);
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = offlineTarget.getUniqueId();

        try {
            int hearts = Integer.parseInt(heartsStr);
            if (hearts < 0 || hearts > plugin.getMaxHearts()) {
                sender.sendMessage(ChatColor.RED + "Hearts must be between 0 and " + plugin.getMaxHearts());
                return;
            }

            DatabaseManager db = plugin.getDatabaseManager();
            db.setHearts(targetUUID, targetName, hearts);

            if (target != null && target.isOnline()) {
                double health = hearts * 2.0;
                AttributeInstance maxHealthAttribute = target.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealthAttribute != null) {
                    maxHealthAttribute.setBaseValue(health);
                    target.setHealth(Math.min(target.getHealth(), health));
                }
                target.sendMessage(ChatColor.YELLOW + "Your hearts have been set to " + hearts);
            }

            sender.sendMessage(ChatColor.GREEN + "Hearts of " + targetName + " set to " + hearts);

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number of hearts!");
        }
    }

    @SuppressWarnings("deprecation")
    private void handleGetHearts(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        DatabaseManager db = plugin.getDatabaseManager();

        int hearts = db.getHearts(target.getUniqueId());
        if (hearts == -1) {
            sender.sendMessage(ChatColor.RED + "Player not found in the database!");
        } else {
            sender.sendMessage(ChatColor.YELLOW + targetName + " has " + ChatColor.RED + hearts +
                    ChatColor.YELLOW + " hearts remaining");
        }
    }

    @SuppressWarnings("deprecation")
    private void handleEliminate(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);

        DatabaseManager db = plugin.getDatabaseManager();
        db.setHearts(offlineTarget.getUniqueId(), targetName, 0);
        db.setBanned(offlineTarget.getUniqueId(), true);

        if (target != null && target.isOnline()) {
            target.kickPlayer(ChatColor.RED + "You have been eliminated by an administrator!");
        }

        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName,
                ChatColor.RED + "Eliminated - 0 hearts remaining", null, null);

        sender.sendMessage(ChatColor.GREEN + "Player " + targetName + " successfully eliminated!");
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "☠ " + ChatColor.RED + targetName +
                " has been ELIMINATED by an administrator!");
    }

    @SuppressWarnings("deprecation")
    private void handleRevive(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        DatabaseManager db = plugin.getDatabaseManager();
        db.setHearts(target.getUniqueId(), targetName, plugin.getMaxHearts());
        db.setBanned(target.getUniqueId(), false);

        Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);

        sender.sendMessage(ChatColor.GREEN + "Player " + targetName + " successfully revived!");
        Bukkit.broadcastMessage(ChatColor.GREEN + "✦ " + targetName +
                " has been revived with " + plugin.getMaxHearts() + " hearts!");
    }
}
