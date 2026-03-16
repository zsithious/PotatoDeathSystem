package com.potatoesnw.PotatoDeathSystem;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class PluginCommand implements CommandExecutor, TabCompleter {

    private final PotatoDeathSystem plugin;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm");

    public PluginCommand(PotatoDeathSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return handleHistory(sender, null);
        }

        switch (args[0].toLowerCase()) {
            case "history" -> {
                String target = args.length > 1 ? args[1] : null;
                handleHistory(sender, target);
            }
            case "top" -> handleTop(sender);
            case "reload" -> handleReload(sender);
            case "help" -> handleHelp(sender);
            default -> handleHelp(sender);
        }

        return true;
    }

    private boolean handleHistory(CommandSender sender, String targetName) {
        if (targetName == null) {
            // Show own history
            if (!(sender instanceof Player player)) {
                plugin.getMessageManager().send(sender, "command.help", Map.of());
                return true;
            }
            if (!player.hasPermission("deathsystem.history")) {
                plugin.getMessageManager().send(sender, "command.no-permission", Map.of());
                return true;
            }

            List<DeathRecord> history = plugin.getDeathLog().getHistory(player.getUniqueId());
            if (history.isEmpty()) {
                plugin.getMessageManager().send(sender, "command.no-deaths", Map.of());
                return true;
            }

            plugin.getMessageManager().sendRaw(sender, "command.history-header",
                    Map.of("for_player", ""));
            sendHistoryEntries(sender, history);
        } else {
            // Show other player's history
            if (!sender.hasPermission("deathsystem.history.others")) {
                plugin.getMessageManager().send(sender, "command.no-permission", Map.of());
                return true;
            }

            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                plugin.getMessageManager().send(sender, "command.player-not-found", Map.of());
                return true;
            }

            List<DeathRecord> history = plugin.getDeathLog().getHistory(target.getUniqueId());
            if (history.isEmpty()) {
                plugin.getMessageManager().send(sender, "command.no-deaths-other",
                        Map.of("player", target.getName()));
                return true;
            }

            plugin.getMessageManager().sendRaw(sender, "command.history-header",
                    Map.of("for_player", " for " + target.getName()));
            sendHistoryEntries(sender, history);
        }

        return true;
    }

    private void sendHistoryEntries(CommandSender sender, List<DeathRecord> history) {
        int maxShow = Math.min(history.size(), 10);
        for (int i = 0; i < maxShow; i++) {
            DeathRecord record = history.get(i);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("index", String.valueOf(i + 1));
            placeholders.put("cause", formatCause(record.getCause()));
            placeholders.put("killer", record.getKillerName());
            placeholders.put("x", String.valueOf(record.getX()));
            placeholders.put("y", String.valueOf(record.getY()));
            placeholders.put("z", String.valueOf(record.getZ()));
            placeholders.put("world", record.getWorld());
            placeholders.put("time", DATE_FORMAT.format(new Date(record.getTimestamp())));

            String key = record.isPvpDeath() ? "command.history-entry-pvp" : "command.history-entry";
            plugin.getMessageManager().sendRaw(sender, key, placeholders);
        }
    }

    private void handleTop(CommandSender sender) {
        if (!sender.hasPermission("deathsystem.top")) {
            plugin.getMessageManager().send(sender, "command.no-permission", Map.of());
            return;
        }

        List<Map.Entry<String, Integer>> top = plugin.getDeathLog().getTopDeaths(10);
        if (top.isEmpty()) {
            plugin.getMessageManager().send(sender, "command.no-deaths", Map.of());
            return;
        }

        plugin.getMessageManager().sendRaw(sender, "command.top-header", Map.of());

        for (int i = 0; i < top.size(); i++) {
            Map.Entry<String, Integer> entry = top.get(i);
            plugin.getMessageManager().sendRaw(sender, "command.top-entry", Map.of(
                    "rank", String.valueOf(i + 1),
                    "player", entry.getKey(),
                    "count", String.valueOf(entry.getValue())
            ));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("deathsystem.reload")) {
            plugin.getMessageManager().send(sender, "command.no-permission", Map.of());
            return;
        }

        plugin.reloadPluginConfig();
        plugin.getMessageManager().send(sender, "command.reload-success", Map.of());
    }

    private void handleHelp(CommandSender sender) {
        plugin.getMessageManager().sendRaw(sender, "command.help", Map.of());
    }

    private String formatCause(String cause) {
        if (cause == null || cause.isEmpty()) return "Unknown";
        String formatted = cause.replace("_", " ").toLowerCase();
        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> subs = List.of("history", "top", "reload", "help");
            for (String sub : subs) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2 && "history".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("deathsystem.history.others")) return Collections.emptyList();
            List<String> players = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    players.add(online.getName());
                }
            }
            return players;
        }

        return Collections.emptyList();
    }
}
