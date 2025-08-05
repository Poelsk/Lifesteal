package com.Lino.lifesteal;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LifestealTabCompleter implements TabCompleter {

    private final Lifesteal plugin;

    public LifestealTabCompleter(Lifesteal plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("help");
            subCommands.add("get");

            if (sender.hasPermission("lifesteal.admin")) {
                subCommands.add("set");
                subCommands.add("eliminate");
                subCommands.add("revive");
                subCommands.add("doomsday");
            }

            if (sender.hasPermission("lifesteal.reload")) {
                subCommands.add("reload");
            }

            String input = args[0].toLowerCase();
            completions = subCommands.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }
        else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "get":
                case "eliminate":
                case "revive":
                case "set":
                    String input = args[1].toLowerCase();
                    completions = getOnlinePlayerNames(input);
                    break;
            }
        }
        else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("set")) {
                List<String> heartSuggestions = new ArrayList<>();
                heartSuggestions.add("0");
                heartSuggestions.add("1");
                heartSuggestions.add("5");
                heartSuggestions.add("10");
                heartSuggestions.add("15");
                heartSuggestions.add("20");
                heartSuggestions.add(String.valueOf(plugin.getStartingHearts()));
                heartSuggestions.add(String.valueOf(plugin.getMaxHearts()));

                String input = args[2];
                completions = heartSuggestions.stream()
                        .distinct()
                        .filter(s -> s.startsWith(input))
                        .sorted((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }

    private List<String> getOnlinePlayerNames(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input))
                .sorted()
                .collect(Collectors.toList());
    }
}