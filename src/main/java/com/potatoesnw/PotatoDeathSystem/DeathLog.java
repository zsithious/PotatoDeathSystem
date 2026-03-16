package com.potatoesnw.PotatoDeathSystem;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DeathLog {

    private final PotatoDeathSystem plugin;
    private final File dataFile;
    private YamlConfiguration data;
    private final ConcurrentHashMap<UUID, List<DeathRecord>> cache = new ConcurrentHashMap<>();
    private BukkitRunnable saveTask;

    public DeathLog(PotatoDeathSystem plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "deaths.yml");
        load();
        startAutoSave();
    }

    private void load() {
        if (!dataFile.exists()) {
            data = new YamlConfiguration();
            return;
        }

        data = YamlConfiguration.loadConfiguration(dataFile);
        cache.clear();

        ConfigurationSection playersSection = data.getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidStr : playersSection.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            List<DeathRecord> records = new ArrayList<>();
            ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidStr);
            if (playerSection == null) continue;

            ConfigurationSection deathsSection = playerSection.getConfigurationSection("deaths");
            if (deathsSection == null) continue;

            for (String key : deathsSection.getKeys(false)) {
                ConfigurationSection deathSection = deathsSection.getConfigurationSection(key);
                if (deathSection == null) continue;

                records.add(new DeathRecord(
                        uuid,
                        deathSection.getString("name", "Unknown"),
                        deathSection.getString("cause", "UNKNOWN"),
                        deathSection.getString("killer", ""),
                        deathSection.getString("world", "world"),
                        deathSection.getInt("x", 0),
                        deathSection.getInt("y", 0),
                        deathSection.getInt("z", 0),
                        deathSection.getLong("time", 0)
                ));
            }

            cache.put(uuid, records);
        }
    }

    public void save() {
        data = new YamlConfiguration();

        for (Map.Entry<UUID, List<DeathRecord>> entry : cache.entrySet()) {
            String basePath = "players." + entry.getKey().toString();
            List<DeathRecord> records = entry.getValue();

            if (!records.isEmpty()) {
                data.set(basePath + ".name", records.get(records.size() - 1).getPlayerName());
            }

            for (int i = 0; i < records.size(); i++) {
                DeathRecord record = records.get(i);
                String deathPath = basePath + ".deaths." + i;
                data.set(deathPath + ".name", record.getPlayerName());
                data.set(deathPath + ".cause", record.getCause());
                data.set(deathPath + ".killer", record.getKillerName());
                data.set(deathPath + ".world", record.getWorld());
                data.set(deathPath + ".x", record.getX());
                data.set(deathPath + ".y", record.getY());
                data.set(deathPath + ".z", record.getZ());
                data.set(deathPath + ".time", record.getTimestamp());
            }
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save death log: " + e.getMessage());
        }
    }

    private void startAutoSave() {
        int interval = plugin.getConfigManager().getSaveInterval();
        if (interval <= 0) return;

        saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                save();
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("[Debug] Auto-saved death log.");
                }
            }
        };
        saveTask.runTaskTimerAsynchronously(plugin, interval * 20L, interval * 20L);
    }

    public void stopAutoSave() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
    }

    public void addDeath(DeathRecord record) {
        List<DeathRecord> records = cache.computeIfAbsent(record.getPlayerUUID(), k -> new ArrayList<>());
        records.add(record);

        int maxHistory = plugin.getConfigManager().getMaxHistory();
        while (records.size() > maxHistory) {
            records.remove(0);
        }
    }

    public List<DeathRecord> getHistory(UUID uuid) {
        List<DeathRecord> records = cache.get(uuid);
        if (records == null) return Collections.emptyList();
        List<DeathRecord> reversed = new ArrayList<>(records);
        Collections.reverse(reversed);
        return reversed;
    }

    public int getDeathCount(UUID uuid) {
        List<DeathRecord> records = cache.get(uuid);
        return records != null ? records.size() : 0;
    }

    public List<Map.Entry<String, Integer>> getTopDeaths(int limit) {
        Map<String, Integer> counts = new HashMap<>();

        for (Map.Entry<UUID, List<DeathRecord>> entry : cache.entrySet()) {
            List<DeathRecord> records = entry.getValue();
            if (!records.isEmpty()) {
                String name = records.get(records.size() - 1).getPlayerName();
                counts.put(name, records.size());
            }
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void reload() {
        stopAutoSave();
        load();
        startAutoSave();
    }
}
