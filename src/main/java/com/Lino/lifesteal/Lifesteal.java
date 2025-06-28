package com.Lino.lifesteal;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class Lifesteal extends JavaPlugin {

    private DatabaseManager databaseManager;
    private MessageManager messageManager;
    private int maxHearts;
    private int startingHearts;
    private boolean loseHeartsFromFall;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        messageManager = new MessageManager(this);
        databaseManager = new DatabaseManager(new File(getDataFolder(), "lifesteal.db"));
        databaseManager.initialize();

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new HeartItemListener(this), this);
        getCommand("lifesteal").setExecutor(new LifestealCommand(this));

        Bukkit.getConsoleSender().sendMessage(messageManager.getMessage("plugin-enabled"));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        Bukkit.getConsoleSender().sendMessage(messageManager.getMessage("plugin-disabled"));
    }

    private void loadConfigValues() {
        maxHearts = getConfig().getInt("max-hearts", 20);
        startingHearts = getConfig().getInt("starting-hearts", 10);
        loseHeartsFromFall = getConfig().getBoolean("lose-hearts-from-fall", false);
    }

    public void reloadPlugin() {
        reloadConfig();
        loadConfigValues();
        messageManager.reload();
        Bukkit.getConsoleSender().sendMessage(messageManager.getMessage("plugin-reloaded"));
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public int getMaxHearts() {
        return maxHearts;
    }

    public int getStartingHearts() {
        return startingHearts;
    }

    public boolean shouldLoseHeartsFromFall() {
        return loseHeartsFromFall;
    }
}