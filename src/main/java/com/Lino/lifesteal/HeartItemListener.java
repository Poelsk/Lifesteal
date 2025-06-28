package com.Lino.lifesteal;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeartItemListener implements Listener {

    private final Lifesteal plugin;

    public HeartItemListener(Lifesteal plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.NETHER_STAR) {
            return;
        }

        if (!isHeartItem(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        DatabaseManager db = plugin.getDatabaseManager();
        MessageManager messages = plugin.getMessageManager();

        int currentHearts = db.getHearts(player.getUniqueId());
        if (currentHearts == -1) {
            currentHearts = plugin.getStartingHearts();
        }

        if (currentHearts >= plugin.getMaxHearts()) {
            player.sendMessage(messages.getMessage("max-hearts-reached"));
            return;
        }

        int newHearts = currentHearts + 1;
        db.setHearts(player.getUniqueId(), player.getName(), newHearts);
        updatePlayerHearts(player, newHearts);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().remove(item);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%hearts%", String.valueOf(newHearts));
        placeholders.put("%maxhearts%", String.valueOf(plugin.getMaxHearts()));
        player.sendMessage(messages.getMessage("heart-used", placeholders));
    }

    public static ItemStack createHeartItem(Lifesteal plugin) {
        ItemStack heart = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = heart.getItemMeta();

        MessageManager messages = plugin.getMessageManager();
        meta.setDisplayName(messages.getMessage("heart-item-name"));

        List<String> lore = new ArrayList<>();
        lore.add(messages.getMessage("heart-item-lore"));
        meta.setLore(lore);

        heart.setItemMeta(meta);
        return heart;
    }

    private boolean isHeartItem(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return false;

        String heartName = plugin.getMessageManager().getMessage("heart-item-name");
        return meta.getDisplayName().equals(heartName);
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