package com.potatoesnw.PotatoDeathSystem;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MessageManager {

    private final PotatoDeathSystem plugin;
    private FileConfiguration messagesConfig;

    public MessageManager(PotatoDeathSystem plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String getRaw(String key) {
        return messagesConfig.getString(key, "&cMissing message: " + key);
    }

    public String format(String key, Map<String, String> placeholders) {
        String message = getRaw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return colorize(message);
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String prefix = colorize(plugin.getConfigManager().getPrefix());
        String message = format(key, placeholders);
        for (String line : message.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                sender.sendMessage(prefix + trimmed);
            }
        }
    }

    public void sendRaw(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = format(key, placeholders);
        for (String line : message.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                sender.sendMessage(trimmed);
            }
        }
    }

    public String getRandomDeathMessage(DamageCause cause, Map<String, String> placeholders) {
        String causeKey = "death-messages." + cause.name();
        List<String> messages = messagesConfig.getStringList(causeKey);

        if (messages.isEmpty()) {
            messages = messagesConfig.getStringList("death-messages.DEFAULT");
        }

        if (messages.isEmpty()) {
            return colorize("&e" + placeholders.getOrDefault("player", "Unknown") + " &7died");
        }

        String selected = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            selected = selected.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return colorize(selected);
    }

    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
