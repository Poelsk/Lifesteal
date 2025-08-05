package com.Lino.lifesteal;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;

public class DeathListener implements Listener {

    private final Lifesteal plugin;

    public DeathListener(Lifesteal plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        DatabaseManager db = plugin.getDatabaseManager();
        MessageManager messages = plugin.getMessageManager();

        if (db.isBanned(player.getUniqueId())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.kickPlayer(messages.getMessage("elimination-kick"));
                }
            }.runTaskLater(plugin, 1L);
            return;
        }

        int hearts = db.getHearts(player.getUniqueId());
        if (hearts == -1) {
            hearts = plugin.getStartingHearts();
            db.createPlayer(player.getUniqueId(), player.getName(), hearts);
        }

        updatePlayerHearts(player, hearts);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        MessageManager messages = plugin.getMessageManager();

        if (!plugin.shouldLoseHeartsFromFall() && victim.getLastDamageCause() != null &&
                victim.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        DatabaseManager db = plugin.getDatabaseManager();
        int currentHearts = db.getHearts(victim.getUniqueId());
        if (currentHearts == -1) {
            currentHearts = plugin.getStartingHearts();
        }

        int newHearts = currentHearts - 1;

        if (newHearts <= 0) {
            db.setHearts(victim.getUniqueId(), victim.getName(), 0);
            db.setBanned(victim.getUniqueId(), true);

            event.setDeathMessage(null);

            Bukkit.broadcastMessage(messages.getMessage("elimination-broadcast", "%player%", victim.getName()));

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.8f, 0.8f);
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.7f);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);
                    }
                }
            }.runTaskLater(plugin, 20L);

            new BukkitRunnable() {
                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    victim.kickPlayer(messages.getMessage("elimination-kick"));
                    Bukkit.getBanList(BanList.Type.NAME).addBan(victim.getName(),
                            messages.getMessage("ban-reason"), null, null);
                }
            }.runTaskLater(plugin, 40L);

        } else {
            db.setHearts(victim.getUniqueId(), victim.getName(), newHearts);

            event.setDeathMessage(null);

            String killerName = killer != null ? killer.getName() : "someone";
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%victim%", victim.getName());
            placeholders.put("%killer%", killerName);
            placeholders.put("%hearts%", String.valueOf(newHearts));
            placeholders.put("%maxhearts%", String.valueOf(plugin.getMaxHearts()));

            Bukkit.broadcastMessage(messages.getMessage("death-message", placeholders));

            victim.getWorld().dropItemNaturally(victim.getLocation(), HeartItemListener.createHeartItem(plugin));
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