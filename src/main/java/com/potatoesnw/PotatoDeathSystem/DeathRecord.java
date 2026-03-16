package com.potatoesnw.PotatoDeathSystem;

import java.util.UUID;

public class DeathRecord {

    private final UUID playerUUID;
    private final String playerName;
    private final String cause;
    private final String killerName;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final long timestamp;

    public DeathRecord(UUID playerUUID, String playerName, String cause, String killerName,
                       String world, int x, int y, int z, long timestamp) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.cause = cause;
        this.killerName = killerName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getCause() {
        return cause;
    }

    public String getKillerName() {
        return killerName;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isPvpDeath() {
        return killerName != null && !killerName.isEmpty();
    }
}
