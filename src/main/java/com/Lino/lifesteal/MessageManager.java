package com.Lino.lifesteal;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {

    private final Lifesteal plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private final Pattern hexPattern = Pattern.compile("#[a-fA-F0-9]{6}");
    private final Pattern gradientPattern = Pattern.compile("<gradient:(#[a-fA-F0-9]{6}):(#[a-fA-F0-9]{6})>(.*?)</gradient>");

    public MessageManager(Lifesteal plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            messagesConfig.setDefaults(defConfig);
        }
    }

    public void reload() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getMessage(String key) {
        String message = messagesConfig.getString(key, key);
        return applyColors(message);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = messagesConfig.getString(key, key);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        return applyColors(message);
    }

    public String getMessage(String key, String placeholder, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder, value);
        return getMessage(key, placeholders);
    }

    private String applyColors(String message) {
        message = applyGradients(message);
        message = applyHexColors(message);
        message = ChatColor.translateAlternateColorCodes('&', message);
        return message;
    }

    private String applyGradients(String message) {
        Matcher matcher = gradientPattern.matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String startColor = matcher.group(1);
            String endColor = matcher.group(2);
            String text = matcher.group(3);

            String gradientText = applyGradient(text, startColor, endColor);
            matcher.appendReplacement(result, Matcher.quoteReplacement(gradientText));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String applyGradient(String text, String startHex, String endHex) {
        if (text.isEmpty()) return text;

        int length = text.length();
        StringBuilder gradientText = new StringBuilder();

        int startR = Integer.parseInt(startHex.substring(1, 3), 16);
        int startG = Integer.parseInt(startHex.substring(3, 5), 16);
        int startB = Integer.parseInt(startHex.substring(5, 7), 16);

        int endR = Integer.parseInt(endHex.substring(1, 3), 16);
        int endG = Integer.parseInt(endHex.substring(3, 5), 16);
        int endB = Integer.parseInt(endHex.substring(5, 7), 16);

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / Math.max(1, length - 1);

            int r = Math.round(startR + ratio * (endR - startR));
            int g = Math.round(startG + ratio * (endG - startG));
            int b = Math.round(startB + ratio * (endB - startB));

            String hex = String.format("#%02x%02x%02x", r, g, b);
            gradientText.append(ChatColor.of(hex)).append(text.charAt(i));
        }

        return gradientText.toString();
    }

    private String applyHexColors(String message) {
        Matcher matcher = hexPattern.matcher(message);

        while (matcher.find()) {
            String hex = matcher.group();
            message = message.replace(hex, ChatColor.of(hex).toString());
        }

        return message;
    }
}