package com.potatoesnw.PotatoDeathSystem;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GravestoneManager {

    private final PotatoDeathSystem plugin;
    private final ConcurrentHashMap<UUID, GravestoneData> gravestones = new ConcurrentHashMap<>();
    private BukkitRunnable cleanupTask;

    public GravestoneManager(PotatoDeathSystem plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    public void spawnGravestone(Player player, List<ItemStack> items, Location deathLocation) {
        if (!plugin.getConfigManager().isGravestoneEnabled()) return;
        if (items.isEmpty()) return;

        Location safeLoc = deathLocation.clone();
        safeLoc.setX(safeLoc.getBlockX() + 0.5);
        safeLoc.setZ(safeLoc.getBlockZ() + 0.5);

        // Ensure location is safe (not in void)
        if (safeLoc.getY() < safeLoc.getWorld().getMinHeight()) {
            safeLoc.setY(safeLoc.getWorld().getMinHeight() + 1);
        }

        ArmorStand stand = (ArmorStand) safeLoc.getWorld().spawnEntity(safeLoc, EntityType.ARMOR_STAND);
        stand.setGravity(false);
        stand.setVisible(false);
        stand.setSmall(true);
        stand.setInvulnerable(true);
        stand.setCanPickupItems(false);
        stand.setBasePlate(false);

        if (plugin.getConfigManager().isGravestoneHologram()) {
            stand.setCustomNameVisible(true);
            stand.customName(net.kyori.adventure.text.Component.text(
                    MessageManager.colorize("&c" + player.getName() + "'s &7Gravestone")));
        }

        UUID standUUID = stand.getUniqueId();
        long expireTime = System.currentTimeMillis() + (plugin.getConfigManager().getGravestoneDuration() * 1000L);

        GravestoneData data = new GravestoneData(
                standUUID, player.getUniqueId(), player.getName(),
                items.toArray(new ItemStack[0]), safeLoc, expireTime
        );
        gravestones.put(standUUID, data);

        plugin.getMessageManager().send(player, "gravestone.spawned", Map.of());

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info(String.format("[Debug] Spawned gravestone for %s at %s (%.0f, %.0f, %.0f) with %d items",
                    player.getName(), safeLoc.getWorld().getName(), safeLoc.getX(), safeLoc.getY(), safeLoc.getZ(), items.size()));
        }
    }

    public boolean tryRecover(Player player, UUID armorStandUUID) {
        GravestoneData data = gravestones.get(armorStandUUID);
        if (data == null) return false;

        boolean isOwner = data.ownerUUID().equals(player.getUniqueId());
        boolean hasOtherPerm = player.hasPermission("deathsystem.gravestone.others");

        if (!isOwner && !hasOtherPerm) {
            plugin.getMessageManager().send(player, "gravestone.no-permission", Map.of());
            return true;
        }

        if (data.items().length == 0) {
            plugin.getMessageManager().send(player, "gravestone.empty", Map.of());
            removeGravestone(armorStandUUID);
            return true;
        }

        // Give items to player
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(data.items());

        // Drop overflow items at player's feet
        for (ItemStack item : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }

        if (isOwner) {
            plugin.getMessageManager().send(player, "gravestone.recovered", Map.of());
        } else {
            plugin.getMessageManager().send(player, "gravestone.recovered-other",
                    Map.of("player", data.ownerName()));
        }

        removeGravestone(armorStandUUID);
        return true;
    }

    private void removeGravestone(UUID standUUID) {
        GravestoneData data = gravestones.remove(standUUID);
        if (data == null) return;

        // Remove the armor stand entity
        Bukkit.getScheduler().runTask(plugin, () -> {
            ArmorStand stand = (ArmorStand) Bukkit.getEntity(standUUID);
            if (stand != null && stand.isValid()) {
                stand.remove();
            }
        });
    }

    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<UUID, GravestoneData>> iterator = gravestones.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<UUID, GravestoneData> entry = iterator.next();
                    GravestoneData data = entry.getValue();

                    if (now >= data.expireTime()) {
                        iterator.remove();

                        // Handle expired gravestone on main thread
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            // Drop items if configured
                            if (plugin.getConfigManager().isDropItemsOnExpire() && data.items().length > 0) {
                                Location loc = data.location();
                                if (loc.getWorld() != null) {
                                    for (ItemStack item : data.items()) {
                                        if (item != null) {
                                            loc.getWorld().dropItemNaturally(loc, item);
                                        }
                                    }
                                }

                                // Notify owner
                                Player owner = Bukkit.getPlayer(data.ownerUUID());
                                if (owner != null && owner.isOnline()) {
                                    plugin.getMessageManager().send(owner, "gravestone.expired", Map.of());
                                }
                            } else {
                                Player owner = Bukkit.getPlayer(data.ownerUUID());
                                if (owner != null && owner.isOnline()) {
                                    plugin.getMessageManager().send(owner, "gravestone.expired-lost", Map.of());
                                }
                            }

                            // Remove armor stand
                            ArmorStand stand = (ArmorStand) Bukkit.getEntity(entry.getKey());
                            if (stand != null && stand.isValid()) {
                                stand.remove();
                            }
                        });
                    }
                }
            }
        };
        cleanupTask.runTaskTimer(plugin, 100L, 100L); // Check every 5 seconds
    }

    public void cleanup() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        // Remove all armor stands and drop items
        for (Map.Entry<UUID, GravestoneData> entry : gravestones.entrySet()) {
            GravestoneData data = entry.getValue();
            ArmorStand stand = (ArmorStand) Bukkit.getEntity(entry.getKey());
            if (stand != null && stand.isValid()) {
                // Drop items at gravestone location
                if (data.items().length > 0 && data.location().getWorld() != null) {
                    for (ItemStack item : data.items()) {
                        if (item != null) {
                            data.location().getWorld().dropItemNaturally(data.location(), item);
                        }
                    }
                }
                stand.remove();
            }
        }
        gravestones.clear();
    }

    public boolean isGravestone(UUID entityUUID) {
        return gravestones.containsKey(entityUUID);
    }

    public record GravestoneData(UUID standUUID, UUID ownerUUID, String ownerName,
                                  ItemStack[] items, Location location, long expireTime) {
    }
}
