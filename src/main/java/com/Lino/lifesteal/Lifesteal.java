package com.Lino.lifesteal;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class Lifesteal extends JavaPlugin {

    private DatabaseManager databaseManager;
    private int maxHearts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        maxHearts = getConfig().getInt("max-hearts", 20);

        databaseManager = new DatabaseManager(new File(getDataFolder(), "lifesteal.db"));
        databaseManager.initialize();

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getCommand("lifesteal").setExecutor(new LifestealCommand(this));

        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Lifesteal abilitato!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Lifesteal online!");
    }

    public void reloadPlugin() {
        reloadConfig();
        maxHearts = getConfig().getInt("max-hearts", 20);
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Lifesteal reloaded!");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public int getMaxHearts() {
        return maxHearts;
    }
}