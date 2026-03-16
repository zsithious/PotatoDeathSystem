package com.potatoesnw.PotatoDeathSystem;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PotatoDeathSystem extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private DeathLog deathLog;
    private GravestoneManager gravestoneManager;

    private final ConcurrentHashMap<UUID, Location> pendingCoordinates = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        deathLog = new DeathLog(this);
        gravestoneManager = new GravestoneManager(this);

        // Register command
        PluginCommand command = new PluginCommand(this);
        getCommand("death").setExecutor(command);
        getCommand("death").setTabCompleter(command);

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // bStats
        if (configManager.isMetricsEnabled()) {
            Metrics metrics = new Metrics(this, 28302);
            metrics.addCustomChart(new SimplePie("gravestone_enabled",
                    () -> String.valueOf(configManager.isGravestoneEnabled())));
            metrics.addCustomChart(new SimplePie("custom_messages_enabled",
                    () -> String.valueOf(configManager.isCustomMessagesEnabled())));
        }

        getLogger().info("PotatoDeathSystem enabled! Advanced death system active.");
    }

    @Override
    public void onDisable() {
        if (deathLog != null) {
            deathLog.stopAutoSave();
            deathLog.save();
        }
        if (gravestoneManager != null) {
            gravestoneManager.cleanup();
        }
        getLogger().info("PotatoDeathSystem disabled.");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        saveDefaultConfig();
        messageManager.loadMessages();
        deathLog.reload();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLoc = player.getLocation();

        // Determine death cause and killer
        String causeName = "UNKNOWN";
        String killerName = "";

        if (player.getLastDamageCause() != null) {
            causeName = player.getLastDamageCause().getCause().name();
        }

        Player killer = player.getKiller();
        if (killer != null) {
            killerName = killer.getName();
        }

        // Custom death message
        if (configManager.isCustomMessagesEnabled()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            placeholders.put("killer", killerName.isEmpty() ? "Unknown" : killerName);

            org.bukkit.event.entity.EntityDamageEvent.DamageCause damageCause =
                    player.getLastDamageCause() != null ? player.getLastDamageCause().getCause()
                            : org.bukkit.event.entity.EntityDamageEvent.DamageCause.CUSTOM;

            String deathMessage = messageManager.getRandomDeathMessage(damageCause, placeholders);
            event.deathMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().deserialize(deathMessage));
        }

        // Keep inventory for players with permission
        if (configManager.isKeepInventoryEnabled() && player.hasPermission("deathsystem.keepinv")) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        messageManager.send(player, "keepinventory-active", Map.of());
                    }
                }
            }.runTaskLater(this, 5L);
        } else if (configManager.isGravestoneEnabled()) {
            // Spawn gravestone with items
            List<ItemStack> items = new ArrayList<>(event.getDrops());
            if (!items.isEmpty()) {
                event.getDrops().clear();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        gravestoneManager.spawnGravestone(player, items, deathLoc);
                    }
                }.runTaskLater(this, 1L);
            }
        }

        // Store death coordinates for after respawn
        if (configManager.isShowCoordinates()) {
            pendingCoordinates.put(player.getUniqueId(), deathLoc.clone());
        }

        // Log death
        DeathRecord record = new DeathRecord(
                player.getUniqueId(), player.getName(), causeName, killerName,
                deathLoc.getWorld().getName(),
                deathLoc.getBlockX(), deathLoc.getBlockY(), deathLoc.getBlockZ(),
                System.currentTimeMillis()
        );
        deathLog.addDeath(record);

        if (configManager.isDebugMode()) {
            getLogger().info(String.format("[Debug] %s died by %s at %s (%d, %d, %d)%s",
                    player.getName(), causeName, deathLoc.getWorld().getName(),
                    deathLoc.getBlockX(), deathLoc.getBlockY(), deathLoc.getBlockZ(),
                    killerName.isEmpty() ? "" : " killed by " + killerName));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLoc = pendingCoordinates.remove(player.getUniqueId());

        if (deathLoc != null) {
            int delay = configManager.getCoordinateDelay();
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        messageManager.send(player, "death-coordinates", Map.of(
                                "x", String.valueOf(deathLoc.getBlockX()),
                                "y", String.valueOf(deathLoc.getBlockY()),
                                "z", String.valueOf(deathLoc.getBlockZ()),
                                "world", deathLoc.getWorld().getName()
                        ));
                    }
                }
            }.runTaskLater(this, delay * 20L);
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof ArmorStand)) return;

        if (gravestoneManager.isGravestone(entity.getUniqueId())) {
            event.setCancelled(true);
            Player player = event.getPlayer();

            if (!player.hasPermission("deathsystem.gravestone")) {
                messageManager.send(player, "gravestone.no-permission", Map.of());
                return;
            }

            gravestoneManager.tryRecover(player, entity.getUniqueId());
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DeathLog getDeathLog() {
        return deathLog;
    }

    public GravestoneManager getGravestoneManager() {
        return gravestoneManager;
    }
}
