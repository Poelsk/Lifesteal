package com.Lino.lifesteal;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LifestealCommand implements CommandExecutor {

    private final Lifesteal plugin;

    public LifestealCommand(Lifesteal plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageManager messages = plugin.getMessageManager();

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
                    sender.sendMessage(messages.getMessage("no-permission", "%action%", "reload the plugin"));
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(messages.getMessage("reload-success"));
                break;

            case "set":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(messages.getMessage("no-permission", "%action%", "modify hearts"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(messages.getMessage("usage-set"));
                    return true;
                }
                handleSetHearts(sender, args[1], args[2]);
                break;

            case "get":
                if (args.length < 2) {
                    sender.sendMessage(messages.getMessage("usage-get"));
                    return true;
                }
                handleGetHearts(sender, args[1]);
                break;

            case "eliminate":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(messages.getMessage("no-permission", "%action%", "eliminate players"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(messages.getMessage("usage-eliminate"));
                    return true;
                }
                handleEliminate(sender, args[1]);
                break;

            case "revive":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(messages.getMessage("no-permission", "%action%", "revive players"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(messages.getMessage("usage-revive"));
                    return true;
                }
                handleRevive(sender, args[1]);
                break;

            case "doomsday":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(messages.getMessage("no-permission", "%action%", "spawn the Doomsday Sword"));
                    return true;
                }
                handleDoomsday(sender);
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        MessageManager messages = plugin.getMessageManager();
        sender.sendMessage(messages.getMessage("help-title"));
        sender.sendMessage(messages.getMessage("help-cmd-help"));
        sender.sendMessage(messages.getMessage("help-cmd-get"));

        if (sender.hasPermission("lifesteal.admin")) {
            sender.sendMessage(messages.getMessage("help-cmd-set"));
            sender.sendMessage(messages.getMessage("help-cmd-eliminate"));
            sender.sendMessage(messages.getMessage("help-cmd-revive"));
            sender.sendMessage(messages.getMessage("help-cmd-doomsday"));
        }

        if (sender.hasPermission("lifesteal.reload")) {
            sender.sendMessage(messages.getMessage("help-cmd-reload"));
        }

        sender.sendMessage(messages.getMessage("help-footer"));
    }

    @SuppressWarnings("deprecation")
    private void handleSetHearts(CommandSender sender, String targetName, String heartsStr) {
        MessageManager messages = plugin.getMessageManager();
        Player target = Bukkit.getPlayer(targetName);
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = offlineTarget.getUniqueId();

        try {
            int hearts = Integer.parseInt(heartsStr);
            if (hearts < 0 || hearts > plugin.getMaxHearts()) {
                sender.sendMessage(messages.getMessage("invalid-hearts", "%max%", String.valueOf(plugin.getMaxHearts())));
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
                target.sendMessage(messages.getMessage("hearts-set-player", "%hearts%", String.valueOf(hearts)));
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%player%", targetName);
            placeholders.put("%hearts%", String.valueOf(hearts));
            sender.sendMessage(messages.getMessage("hearts-set", placeholders));

        } catch (NumberFormatException e) {
            sender.sendMessage(messages.getMessage("invalid-number"));
        }
    }

    @SuppressWarnings("deprecation")
    private void handleGetHearts(CommandSender sender, String targetName) {
        MessageManager messages = plugin.getMessageManager();
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        DatabaseManager db = plugin.getDatabaseManager();

        int hearts = db.getHearts(target.getUniqueId());
        if (hearts == -1) {
            sender.sendMessage(messages.getMessage("player-not-found"));
        } else {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%player%", targetName);
            placeholders.put("%hearts%", String.valueOf(hearts));
            sender.sendMessage(messages.getMessage("hearts-display", placeholders));
        }
    }

    @SuppressWarnings("deprecation")
    private void handleEliminate(CommandSender sender, String targetName) {
        MessageManager messages = plugin.getMessageManager();
        Player target = Bukkit.getPlayer(targetName);
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);

        DatabaseManager db = plugin.getDatabaseManager();
        db.setHearts(offlineTarget.getUniqueId(), targetName, 0);
        db.setBanned(offlineTarget.getUniqueId(), true);

        if (target != null && target.isOnline()) {
            target.kickPlayer(messages.getMessage("admin-kick"));
        }

        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName,
                messages.getMessage("ban-reason"), null, null);

        sender.sendMessage(messages.getMessage("eliminate-success", "%player%", targetName));
        Bukkit.broadcastMessage(messages.getMessage("elimination-admin", "%player%", targetName));
    }

    @SuppressWarnings("deprecation")
    private void handleRevive(CommandSender sender, String targetName) {
        MessageManager messages = plugin.getMessageManager();
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        DatabaseManager db = plugin.getDatabaseManager();
        db.setHearts(target.getUniqueId(), targetName, plugin.getMaxHearts());
        db.setBanned(target.getUniqueId(), false);

        Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);

        sender.sendMessage(messages.getMessage("revive-success", "%player%", targetName));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", targetName);
        placeholders.put("%hearts%", String.valueOf(plugin.getMaxHearts()));
        Bukkit.broadcastMessage(messages.getMessage("revive-broadcast", placeholders));
    }

    private void handleDoomsday(CommandSender sender) {
        MessageManager messages = plugin.getMessageManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getMessage("console-cannot-use"));
            return;
        }

        Player player = (Player) sender;
        ItemStack doomsdaySword = DoomsdaySword.createDoomsdaySword(plugin);

        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), doomsdaySword);
            player.sendMessage(messages.getMessage("doomsday-dropped"));
        } else {
            player.getInventory().addItem(doomsdaySword);
            player.sendMessage(messages.getMessage("doomsday-received"));
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.5f);
            p.playSound(p.getLocation(), Sound.AMBIENT_CRIMSON_FOREST_MOOD, 1.0f, 0.5f);
        }

        player.getWorld().strikeLightningEffect(player.getLocation());
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 100, 0.5, 1, 0.5, 0.1);
    }
}