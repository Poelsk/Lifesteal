package com.Lino.lifesteal;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class DoomsdaySword implements Listener {

    private final Lifesteal plugin;
    private final NamespacedKey doomsdayKey;
    private final Map<UUID, BukkitTask> soundTasks = new HashMap<>();

    public DoomsdaySword(Lifesteal plugin) {
        this.plugin = plugin;
        this.doomsdayKey = new NamespacedKey(plugin, "doomsday_sword");
        startGlobalCheck();
    }

    public static ItemStack createDoomsdaySword(Lifesteal plugin) {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        MessageManager messages = plugin.getMessageManager();

        meta.setDisplayName(messages.getMessage("doomsday-sword-name"));

        List<String> lore = new ArrayList<>();
        lore.add(messages.getMessage("doomsday-sword-lore1"));
        lore.add(messages.getMessage("doomsday-sword-lore2"));
        lore.add("");
        lore.add(messages.getMessage("doomsday-sword-lore3"));
        lore.add(messages.getMessage("doomsday-sword-lore4"));
        lore.add("");
        lore.add(messages.getMessage("doomsday-sword-lore5"));
        lore.add(messages.getMessage("doomsday-sword-lore6"));
        meta.setLore(lore);

        meta.addEnchant(Enchantment.SHARPNESS, 10, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
        meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        NamespacedKey key = new NamespacedKey(plugin, "doomsday_sword");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        sword.setItemMeta(meta);
        return sword;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!isDoomsdaySword(weapon)) return;

        event.setDeathMessage(null);

        MessageManager messages = plugin.getMessageManager();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%victim%", victim.getName());
        placeholders.put("%killer%", killer.getName());

        Bukkit.broadcastMessage(messages.getMessage("doomsday-elimination", placeholders));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
            p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 0.7f);
            p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.8f);
        }

        for (int i = 0; i < 3; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (victim.isOnline()) {
                        victim.getWorld().strikeLightningEffect(victim.getLocation());
                    }
                }
            }.runTaskLater(plugin, i * 10L);
        }

        killer.getInventory().remove(weapon);
        killer.sendMessage(messages.getMessage("doomsday-sword-consumed"));

        killer.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, killer.getLocation(), 100, 0.5, 1, 0.5, 0.1);
        killer.getWorld().spawnParticle(Particle.SMOKE, killer.getLocation(), 50, 0.3, 0.5, 0.3, 0.05);

        DatabaseManager db = plugin.getDatabaseManager();
        db.setHearts(victim.getUniqueId(), victim.getName(), 0);
        db.setBanned(victim.getUniqueId(), true);

        new BukkitRunnable() {
            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                victim.kickPlayer(messages.getMessage("doomsday-kick"));
                Bukkit.getBanList(BanList.Type.NAME).addBan(victim.getName(),
                        messages.getMessage("doomsday-ban-reason"), null, null);
            }
        }.runTaskLater(plugin, 60L);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());

        if (isDoomsdaySword(newItem)) {
            startSoundEffect(player);
        } else if (isDoomsdaySword(oldItem)) {
            stopSoundEffect(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isDoomsdaySword(player.getInventory().getItemInMainHand())) {
                    startSoundEffect(player);
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopSoundEffect(event.getPlayer());
    }

    private void startGlobalCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    boolean hasSword = isDoomsdaySword(player.getInventory().getItemInMainHand());
                    boolean hasTask = soundTasks.containsKey(player.getUniqueId());

                    if (hasSword && !hasTask) {
                        startSoundEffect(player);
                    } else if (!hasSword && hasTask) {
                        stopSoundEffect(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startSoundEffect(Player player) {
        if (soundTasks.containsKey(player.getUniqueId())) return;

        BukkitTask task = new BukkitRunnable() {
            int counter = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !isDoomsdaySword(player.getInventory().getItemInMainHand())) {
                    stopSoundEffect(player);
                    return;
                }

                Location loc = player.getLocation();

                if (counter % 2 == 0) {
                    player.getWorld().playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.5f);
                    player.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.2f, 0.3f);
                }

                if (counter % 4 == 0) {
                    player.getWorld().playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.7f);
                }

                if (counter % 6 == 0) {
                    player.getWorld().playSound(loc, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.3f, 0.6f);
                }

                player.getWorld().spawnParticle(Particle.SOUL, loc.add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.01);
                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 1, 0.1, 0.1, 0.1, 0.005);

                counter++;
            }
        }.runTaskTimer(plugin, 0L, 10L);

        soundTasks.put(player.getUniqueId(), task);
    }

    private void stopSoundEffect(Player player) {
        BukkitTask task = soundTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private boolean isDoomsdaySword(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(doomsdayKey, PersistentDataType.BYTE);
    }

    public void cleanup() {
        for (BukkitTask task : soundTasks.values()) {
            task.cancel();
        }
        soundTasks.clear();
    }
}