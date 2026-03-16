package com.potatoesnw.PotatoDeathSystem;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final PotatoDeathSystem plugin;

    public ConfigManager(PotatoDeathSystem plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration config() {
        return plugin.getConfig();
    }

    // Death settings
    public boolean isCustomMessagesEnabled() {
        return config().getBoolean("death.custom-messages", true);
    }

    public boolean isShowCoordinates() {
        return config().getBoolean("death.show-coordinates", true);
    }

    public int getCoordinateDelay() {
        return config().getInt("death.coordinate-delay", 3);
    }

    // Keep inventory settings
    public boolean isKeepInventoryEnabled() {
        return config().getBoolean("keepinventory.enabled", true);
    }

    // Gravestone settings
    public boolean isGravestoneEnabled() {
        return config().getBoolean("gravestone.enabled", true);
    }

    public int getGravestoneDuration() {
        return config().getInt("gravestone.duration", 300);
    }

    public boolean isDropItemsOnExpire() {
        return config().getBoolean("gravestone.drop-items-on-expire", true);
    }

    public boolean isGravestoneHologram() {
        return config().getBoolean("gravestone.hologram", true);
    }

    // Log settings
    public int getMaxHistory() {
        return config().getInt("log.max-history", 20);
    }

    public int getSaveInterval() {
        return config().getInt("log.save-interval", 300);
    }

    // Plugin settings
    public boolean isDebugMode() {
        return config().getBoolean("settings.debug-mode", false);
    }

    public boolean isMetricsEnabled() {
        return config().getBoolean("settings.metrics", true);
    }

    public String getPrefix() {
        return config().getString("messages.prefix", "&4&lDeath &8»&f ");
    }
}
