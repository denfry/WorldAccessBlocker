# WorldAccessBlocker — Minecraft World Access Restriction Plugin

[![Servers](https://img.shields.io/bstats/servers/26810?color=blue&label=Servers)](https://bstats.org/plugin/bukkit/WorldAccessBlocker/26810)
[![Players](https://img.shields.io/bstats/players/26810?color=red&label=Players)](https://bstats.org/plugin/bukkit/WorldAccessBlocker/26810)
[![Version](https://img.shields.io/modrinth/v/worldaccessblocker?label=Modrinth)](https://modrinth.com/plugin/worldaccessblocker)
[![License](https://img.shields.io/github/license/denfry/WorldAccessBlocker)](LICENSE)

**WorldAccessBlocker** is a lightweight Minecraft plugin that restricts player access to the Nether, the End, elytra flight, and custom worlds — by fixed date or recurring weekly schedule. Works natively on Paper, Spigot, Bukkit, Purpur, and Folia.

---

## Features

- **Date-based restrictions** — block access until a specific date/time
- **Recurring schedules** — allow access only on certain days and time windows (e.g. weekends 15:00–22:00)
- **Nether blocker** — cancel portal use and teleportation
- **End blocker** — prevent End portal activation
- **Elytra disable** — block equipping and/or flight
- **Custom world restrictions** — restrict any named world by date or schedule
- **Fallback teleport** — send players to a configurable world on block
- **Per-player bypasses** — grant timed bypass with `/wab bypass`, revoke with `/wab remove`
- **PlaceholderAPI support** — expose restriction state and time-left to scoreboards/TAB
- **Multi-language** — English and Russian included, add your own `.yml`
- **Live reload** — apply config changes without restart via `/wabreload`
- **Folia support** — native Folia scheduler (no thread-safety warnings)
- **Update notifications** — admins see an in-game alert when a new version is available

---

## Compatibility

| Platform | Versions | Status |
|---|---|---|
| Paper | 1.16 – 1.21.4 | ✅ Full support |
| Spigot | 1.16 – 1.21.4 | ✅ Full support |
| Bukkit | 1.16 – 1.21.4 | ✅ Full support |
| Purpur | 1.16 – 1.21.4 | ✅ Full support |
| Folia | 1.20+ | ✅ Native scheduler |

**Requirements:** Java 21+

---

## Installation

1. Download `WorldAccessBlocker.jar` from [Modrinth](https://modrinth.com/plugin/worldaccessblocker) or [GitHub Releases](../../releases).
2. Place it in your server's `plugins/` directory.
3. Restart the server.
4. Edit `plugins/WorldAccessBlocker/config.yml` to configure restrictions.
5. Optionally edit `plugins/WorldAccessBlocker/lang/ru.yml` or `en.yml`.

---

## Commands & Permissions

| Command | Permission | Default | Description |
|---|---|---|---|
| `/wab bypass <player> <feature> <seconds>` | `wab.bypass` | op | Grant a timed bypass |
| `/wab remove <player> <feature>` | `wab.bypass` | op | Remove a bypass |
| `/wab status [player]` | `wab.bypass` | op | Show restriction status |
| `/wabreload` | `wab.reload` | op | Reload config without restart |

| Permission | Default | Description |
|---|---|---|
| `wab.bypass` | op | Grant/remove/view bypasses |
| `wab.reload` | op | Reload plugin config |
| `wab.update-notify` | op | Receive in-game new-version alerts |

---

## How Does the Schedule Work?

When `recurring:` is defined under a feature, `restriction-date` is ignored entirely. The plugin checks whether the current day and time fall inside any configured period — if yes, access is blocked; otherwise it is open.

An **empty** `periods: []` means **always blocked** — useful for locking a world indefinitely.

```yaml
nether:
  disable: true
  recurring:
    periods:
      # Open only on Sunday afternoons
      - days: [SUNDAY]
        start-time: "15:00"
        end-time: "17:00"
```

---

## Config Example

```yaml
language: "ru"
time-zone: "Europe/Moscow"

nether:
  disable: true
  restriction-date: "2025-03-10 00:00:00"
  disable-portal-creation: true
  disable-teleportation: true
  recurring:
    periods:
      - days: [SUNDAY]
        start-time: "15:00"
        end-time: "17:00"

end:
  disable: true
  restriction-date: "2025-04-15 00:00:00"
  disable-portal-activation: true

elytra:
  disable: true
  restriction-date: "2025-05-01 00:00:00"
  disable-equip: true
  disable-flight: true

custom-worlds:
  lobby_world:
    disable: true
    recurring:
      periods:
        - days: [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]
          start-time: "09:00"
          end-time: "17:00"

fallback-spawns:
  default: "world"
  nether: "hub"
  custom-worlds:
    lobby_world: "lobby_spawn"
```

---

## PlaceholderAPI Placeholders

Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) to use these in scoreboards, TAB, chat, etc.:

| Placeholder | Returns |
|---|---|
| `%wab_nether_blocked%` | `true` / `false` |
| `%wab_end_blocked%` | `true` / `false` |
| `%wab_elytra_blocked%` | `true` / `false` |
| `%wab_time_left_nether%` | Human-readable time remaining |
| `%wab_time_left_end%` | Human-readable time remaining |
| `%wab_time_left_elytra%` | Human-readable time remaining |

---

## Frequently Asked Questions

### Does WorldAccessBlocker work with Folia?

Yes. Since v0.9.0, WorldAccessBlocker uses a native `PlatformRuntime` abstraction that dispatches tasks via the Folia regional scheduler when running on Folia, and the standard Bukkit scheduler on Paper/Spigot.

### Can I restrict a world by date and then switch to a schedule later?

Yes. If `recurring:` is present under a feature, the `restriction-date` is ignored. Remove the `recurring:` block to go back to date-based mode and run `/wabreload`.

### How do I give a player permanent access to a restricted feature?

Use a very long bypass duration: `/wab bypass PlayerName nether 999999999`

### Where can I report bugs or request features?

Open an issue on [GitHub](../../issues) or leave a comment on the [Modrinth page](https://modrinth.com/plugin/worldaccessblocker).

---

## Building from Source

```bash
git clone https://github.com/denfry/WorldAccessBlocker.git
cd WorldAccessBlocker
mvn clean package
# Output: target/worldaccessblocker-<version>.jar
```

Requires Java 21 and Maven 3.8+.
