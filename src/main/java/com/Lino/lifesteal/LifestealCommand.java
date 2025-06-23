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
                    sender.sendMessage(ChatColor.RED + "Non hai il permesso per ricaricare il plugin!");
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(ChatColor.GREEN + "Plugin ricaricato con successo!");
                break;

            case "set":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ChatColor.RED + "Non hai il permesso per modificare i cuori!");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /lifesteal set <giocatore> <cuori>");
                    return true;
                }
                handleSetHearts(sender, args[1], args[2]);
                break;

            case "get":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /lifesteal get <giocatore>");
                    return true;
                }
                handleGetHearts(sender, args[1]);
                break;

            case "eliminate":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ChatColor.RED + "Non hai il permesso per eliminare giocatori!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /lifesteal eliminate <giocatore>");
                    return true;
                }
                handleEliminate(sender, args[1]);
                break;

            case "revive":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ChatColor.RED + "Non hai il permesso per resuscitare giocatori!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /lifesteal revive <giocatore>");
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
        sender.sendMessage(ChatColor.YELLOW + "/lifesteal help" + ChatColor.WHITE + " - Mostra questo menu");
        sender.sendMessage(ChatColor.YELLOW + "/lifesteal get <giocatore>" + ChatColor.WHITE + " - Mostra i cuori di un giocatore");

        if (sender.hasPermission("lifesteal.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/lifesteal set <giocatore> <cuori>" + ChatColor.WHITE + " - Imposta i cuori di un giocatore");
            sender.sendMessage(ChatColor.YELLOW + "/lifesteal eliminate <giocatore>" + ChatColor.WHITE + " - Elimina un giocatore");
            sender.sendMessage(ChatColor.YELLOW + "/lifesteal revive <giocatore>" + ChatColor.WHITE + " - Resuscita un giocatore");
        }

        if (sender.hasPermission("lifesteal.reload")) {
            sender.sendMessage(ChatColor.YELLOW + "/lifesteal reload" + ChatColor.WHITE + " - Ricarica la configurazione");
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
                sender.sendMessage(ChatColor.RED + "I cuori devono essere tra 0 e " + plugin.getMaxHearts());
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
                target.sendMessage(ChatColor.YELLOW + "I tuoi cuori sono stati impostati a " + hearts);
            }

            sender.sendMessage(ChatColor.GREEN + "Cuori di " + targetName + " impostati a " + hearts);

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Numero di cuori non valido!");
        }
    }

    @SuppressWarnings("deprecation")
    private void handleGetHearts(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        DatabaseManager db = plugin.getDatabaseManager();

        int hearts = db.getHearts(target.getUniqueId());
        if (hearts == -1) {
            sender.sendMessage(ChatColor.RED + "Giocatore non trovato nel database!");
        } else {
            sender.sendMessage(ChatColor.YELLOW + targetName + " ha " + ChatColor.RED + hearts +
                    ChatColor.YELLOW + " cuori rimanenti");
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
            target.kickPlayer(ChatColor.RED + "Sei stato eliminato da un amministratore!");
        }

        Bukkit.getBanList(BanList.Type.NAME).addBan(targetName,
                ChatColor.RED + "Eliminato - 0 cuori rimanenti", null, null);

        sender.sendMessage(ChatColor.GREEN + "Giocatore " + targetName + " eliminato con successo!");
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "☠ " + ChatColor.RED + targetName +
                " è stato ELIMINATO da un amministratore!");
    }

    @SuppressWarnings("deprecation")
    private void handleRevive(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        DatabaseManager db = plugin.getDatabaseManager();
        db.setHearts(target.getUniqueId(), targetName, plugin.getMaxHearts());
        db.setBanned(target.getUniqueId(), false);

        Bukkit.getBanList(BanList.Type.NAME).pardon(targetName);

        sender.sendMessage(ChatColor.GREEN + "Giocatore " + targetName + " resuscitato con successo!");
        Bukkit.broadcastMessage(ChatColor.GREEN + "✦ " + targetName +
                " è stato resuscitato con " + plugin.getMaxHearts() + " cuori!");
    }
}