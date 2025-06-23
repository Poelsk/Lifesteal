package com.Lino.lifesteal;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathListener implements Listener {

    private final Lifesteal plugin;

    public DeathListener(Lifesteal plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        DatabaseManager db = plugin.getDatabaseManager();

        if (db.isBanned(player.getUniqueId())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.kickPlayer(ChatColor.RED + "Sei stato eliminato! Non hai più cuori.");
                }
            }.runTaskLater(plugin, 1L);
            return;
        }

        int hearts = db.getHearts(player.getUniqueId());
        if (hearts == -1) {
            hearts = plugin.getMaxHearts();
            db.createPlayer(player.getUniqueId(), player.getName(), hearts);
        }

        updatePlayerHearts(player, hearts);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        DatabaseManager db = plugin.getDatabaseManager();
        int currentHearts = db.getHearts(victim.getUniqueId());
        if (currentHearts == -1) {
            currentHearts = plugin.getMaxHearts();
        }

        int newHearts = currentHearts - 1;

        if (newHearts <= 0) {
            db.setHearts(victim.getUniqueId(), victim.getName(), 0);
            db.setBanned(victim.getUniqueId(), true);

            event.setDeathMessage(null);

            Bukkit.broadcastMessage(ChatColor.DARK_RED + "☠ " + ChatColor.RED + victim.getName() +
                    " è stato ELIMINATO PERMANENTEMENTE!");

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
            }

            new BukkitRunnable() {
                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    victim.kickPlayer(ChatColor.RED + "Sei stato eliminato! Non hai più cuori.");
                    Bukkit.getBanList(BanList.Type.NAME).addBan(victim.getName(),
                            ChatColor.RED + "Eliminato - 0 cuori rimanenti", null, null);
                }
            }.runTaskLater(plugin, 1L);

        } else {
            db.setHearts(victim.getUniqueId(), victim.getName(), newHearts);

            event.setDeathMessage(null);

            String killerName = killer != null ? killer.getName() : "qualcuno";
            Bukkit.broadcastMessage(ChatColor.RED + "❤ " + victim.getName() +
                    " è stato ucciso da " + killerName + " e ha perso 1 cuore! " +
                    ChatColor.GRAY + "(" + newHearts + "/" + plugin.getMaxHearts() + " cuori rimanenti)");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                int hearts = plugin.getDatabaseManager().getHearts(player.getUniqueId());
                if (hearts > 0) {
                    updatePlayerHearts(player, hearts);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    private void updatePlayerHearts(Player player, int hearts) {
        double health = hearts * 2.0;
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(health);
            player.setHealth(Math.min(player.getHealth(), health));
        }
    }
}