# 🌍 WorldAccessBlocker

[![Servers](https://img.shields.io/bstats/servers/26810?color=blue&label=Servers)](https://bstats.org/plugin/bukkit/WorldAccessBlocker/26810)
[![Players](https://img.shields.io/bstats/players/26810?color=red&label=Players)](https://bstats.org/plugin/bukkit/WorldAccessBlocker/26810)

**WorldAccessBlocker** is a lightweight and highly configurable Minecraft plugin for **Bukkit**, **Spigot**, **Paper**, and **Folia** servers that lets you **restrict access to the Nether, the End, and elytra usage** until specific dates.

Perfect for managing **progression-based survival**, **seasonal events**, or **timed unlocks** — with multi-language support, date-based control, and live reloads.

---

## ✨ Features

- 🔥 **Restrict Nether Access**  
  Prevent players from entering the Nether until a specified date.

- 🚫 **Block End Portal Activation**  
  Disallow activation of End portals before a configured time.

- 🛑 **Disable Elytra Usage**  
  Cancels gliding and disables elytra flight until allowed.

- 🌍 **Multi-Language Support**  
  Includes `English` and `Russian` by default — customizable via `.yml`.

- 📅 **Date-Based Restrictions**  
  Set unlock dates using `YYYY-MM-DD HH:MM:SS` (UTC).

- ♻️ **Live Reload**  
  Instantly reload configs and language files with `/wabreload`.

- ⚠️ **Folia Compatibility Notices**  
  Alerts admins about partial feature support on Folia servers.

---

## 📦 Installation

1. Download `WorldAccessBlocker.jar`.
2. Place it in your server’s `/plugins` folder.
3. Start or reload the server.
4. Configure `config.yml` and language files in:  
   `plugins/WorldAccessBlocker/lang/`

---

## ⚙️ Configuration Example

```yaml

# ===================================================================
# WorldAccessBlocker v0.7
# ===================================================================

# Message language: "ru" or "en"
language: "ru"

# Server time zone (important for recurring!)
# List: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
time-zone: "Europe/Moscow"

# ===================================================================
# Nether
# ===================================================================
nether:
  disable: true
  # If recurring is NO, this date is used (until when it is blocked)
  restriction-date: “2025-03-10 00:00:00”
  disable-portal-creation: true
  disable-teleportation: true

  # NEW: schedule by day and time
  # If this block is present, restriction-date is IGNORED
  recurring:
    periods:
      # Example 1: only on Sundays from 3:00 p.m. to 5:00 p.m.
      - days: [SUNDAY]
        start-time: “3:00 p.m.”
        end-time: “5:00 p.m.”

      # Example 2: all day on weekends
      - days: [SATURDAY, SUNDAY]

      # Example 3: every Monday from 6:00 p.m. to 11:59 p.m.
      - days: [MONDAY]
        start-time: “6:00 p.m.”
        end-time: “11:59 p.m.”
       
```
