# PotatoDeathSystem

<div align="center">

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/zSithious/PotatoDeathSystem/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-green.svg)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://adoptium.net)
[![License](https://img.shields.io/badge/license-MIT-lightgrey.svg)](LICENSE)

**Advanced death system with custom messages, gravestones, and death history.**

[Download](https://github.com/zSithious/PotatoDeathSystem/releases/latest) · [Modrinth](https://modrinth.com/plugin/potatodeathsystem) · [Report Bug](https://github.com/zSithious/PotatoDeathSystem/issues)

</div>

---

## Features

- **Custom Death Messages** — 20+ death causes with multiple random messages each
- **Death Coordinates** — Players receive their death location after respawning
- **Gravestone System** — Items are stored in a gravestone at the death location, recoverable by right-click
- **Keep Inventory** — Permission-based keep inventory (`deathsystem.keepinv`)
- **Death History** — View recent deaths with cause, location, and time
- **Death Leaderboard** — See who dies the most on the server
- **Timed Gravestones** — Gravestones despawn after configurable duration, optionally dropping items
- **Fully Customizable** — Every message editable in `messages.yml` with color codes
- **Persistent Storage** — Death history saved to `deaths.yml` with auto-save
- **bStats Metrics** — Optional anonymous usage statistics

---

## Installation

1. Download the latest `.jar` from [Releases](https://github.com/zSithious/PotatoDeathSystem/releases/latest)
2. Place it in your server's `plugins/` folder
3. Start the server
4. Edit `plugins/PotatoDeathSystem/config.yml` and `messages.yml`

<details>
<summary>Build from source</summary>

```bash
git clone https://github.com/zSithious/PotatoDeathSystem.git
cd PotatoDeathSystem
mvn clean package
```

The compiled JAR is at `target/PotatoDeathSystem-1.0.0.jar`.
</details>

---

## Configuration

### `config.yml`
```yaml
death:
  custom-messages: true         # Enable random death messages
  show-coordinates: true        # Show death coords after respawn
  coordinate-delay: 3           # Delay in seconds

keepinventory:
  enabled: true                 # Permission-based keep inventory

gravestone:
  enabled: true                 # Enable gravestone system
  duration: 300                 # Seconds before despawn
  drop-items-on-expire: true    # Drop items when gravestone expires
  hologram: true                # Floating name above gravestone

log:
  max-history: 20               # Deaths to keep per player
  save-interval: 300            # Auto-save interval (seconds)

settings:
  debug-mode: false
  metrics: true
```

### `messages.yml`
All messages and death messages are fully customizable. Add your own messages per death cause.

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/death` | Show your death history | `deathsystem.history` |
| `/death history [player]` | View death history | `deathsystem.history` / `.others` |
| `/death top` | Death leaderboard | `deathsystem.top` |
| `/death reload` | Reload configuration | `deathsystem.reload` |
| `/death help` | Help menu | — |

**Aliases:** `/pds`, `/deaths`

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `deathsystem.admin` | All commands | OP |
| `deathsystem.keepinv` | Keep inventory on death | false |
| `deathsystem.history` | View own death history | true |
| `deathsystem.history.others` | View others' history | OP |
| `deathsystem.top` | View leaderboard | true |
| `deathsystem.gravestone` | Recover own gravestone | true |
| `deathsystem.gravestone.others` | Recover others' gravestones | OP |
| `deathsystem.reload` | Reload config | OP |

---

## How It Works

1. **Death Event** — When a player dies, the plugin logs the death, sends a custom message to chat, and spawns a gravestone with the player's items
2. **Gravestone** — An invisible armor stand with a floating name tag marks the death location. Right-click to recover items. Admins can recover anyone's gravestone
3. **Respawn** — After respawning, the player receives a message with their death coordinates
4. **Keep Inventory** — Players with `deathsystem.keepinv` permission keep all items and XP on death
5. **Expiry** — Gravestones despawn after the configured duration. Items are dropped on the ground or lost based on config

---

## Death Messages

The plugin includes unique messages for 20+ death causes:
- Entity attacks, projectiles, sweep attacks
- Fall, lava, fire, drowning, void
- Explosions, suffocation, starvation
- Poison, wither, magic, lightning
- Thorns, cramming, kinetic energy, freezing, sonic boom
- Cactus contact and more

Each cause has multiple messages — one is selected randomly each time.

---

## Requirements

- **Minecraft:** 1.21.4
- **Server:** Paper / Spigot / Bukkit
- **Java:** 21+

---

## Metrics (bStats)

This plugin collects anonymous usage statistics. No personal data is collected. To disable, set `settings.metrics: false` in `config.yml`.

---

## Bug Reports / Feature Requests

Please open an issue on [GitHub Issues](https://github.com/zSithious/PotatoDeathSystem/issues).

---

### Developer

**zSithious** — [GitHub](https://github.com/zSithious)

### License

This project is licensed under the [MIT License](LICENSE).
