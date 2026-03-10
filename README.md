# WorldAccessBlocker

[![Servers](https://img.shields.io/bstats/servers/26810?color=blue&label=Servers)](https://bstats.org/plugin/bukkit/WorldAccessBlocker/26810)
[![Players](https://img.shields.io/bstats/players/26810?color=red&label=Players)](https://bstats.org/plugin/bukkit/WorldAccessBlocker/26810)

WorldAccessBlocker is a lightweight and configurable Minecraft plugin for Bukkit, Spigot, Paper, and Folia servers.
It restricts access to the Nether, the End, elytra usage, and custom worlds until specific dates or by recurring schedules.

## Features

- Restrict Nether access
- Block End portal activation
- Disable elytra equip and flight
- Date-based restrictions
- Recurring schedules by day/time
- Custom world restrictions
- Multi-language messages (`en`, `ru`)
- Live config reload (`/wabreload`)
- Player bypasses with expiration
- PlaceholderAPI integration (soft dependency)

## Installation

1. Download `WorldAccessBlocker.jar`.
2. Put it into the server `/plugins` directory.
3. Start the server.
4. Configure `plugins/WorldAccessBlocker/config.yml` and `plugins/WorldAccessBlocker/lang/*.yml`.

## Commands

- `/wab bypass <player|uuid> <feature> <seconds>`
- `/wab remove <player|uuid> <feature>`
- `/wab status [player|uuid]`
- `/wabreload`

## PlaceholderAPI

When PlaceholderAPI is installed, these placeholders are available:

- `%wab_nether_blocked%`
- `%wab_end_blocked%`
- `%wab_elytra_blocked%`
- `%wab_time_left_nether%`
- `%wab_time_left_end%`
- `%wab_time_left_elytra%`

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
  end: "world"
  custom-worlds:
    lobby_world: "lobby_spawn"
```
