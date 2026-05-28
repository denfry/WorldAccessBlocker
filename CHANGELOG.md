# Changelog

All notable changes to WorldAccessBlocker are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [0.9.0] — 2026-05-28

### Added

- **Version checker** — on startup the plugin asynchronously queries the Modrinth API and notifies operators with the `wab.update-notify` permission when a newer release is available. Notification appears once per join session.
- **`wab.update-notify` permission** — dedicated permission for update notifications, separate from `wab.reload`. Default: op.
- **Folia-native scheduler** (`PlatformRuntime`) — new `runtime/` abstraction layer dispatches tasks via the Folia regional scheduler on Folia servers and the standard Bukkit scheduler on Paper/Spigot. Eliminates cross-thread teleport warnings on Folia.
- **`WorldEntryBlocker` listener** — handles `PlayerTeleportEvent` and `PlayerChangedWorldEvent` to block custom-world entry on teleport, in addition to the periodic enforcer sweep.
- **CI/CD — build workflow** (`.github/workflows/build.yml`) — builds and runs all tests automatically on every push and pull request to `master`.
- **CI/CD — release workflow** (`.github/workflows/release.yml`) — on `v*` tag push: builds the JAR, creates a GitHub Release, and uploads to Modrinth automatically.

### Changed

- `VersionChecker` and `RestrictionEnforcer` now use `PlatformRuntime` instead of `Bukkit.getScheduler()` directly.
- Folia detection moved to `PlatformRuntimeFactory.isFoliaRuntime()` using class-presence check.
- Log message on Folia detection changed from a warning to an info message.

### Fixed

- `VersionChecker`: `latestVersion` field made `private volatile` — closes accidental external mutation.
- `VersionChecker`: error stream now properly drained and closed on non-200 HTTP responses, preventing socket leaks.
- `VersionChecker`: `parseSemver` strips pre-release labels (e.g. `-SNAPSHOT`) before parsing — avoids silent `null` on dev builds.

---

## [0.8.0] — 2025-XX-XX

### Added

- `/wab status [player|uuid]` command to view restriction status per player.
- PlaceholderAPI support (soft dependency). New placeholders:
  - `%wab_nether_blocked%`
  - `%wab_end_blocked%`
  - `%wab_elytra_blocked%`
  - `%wab_time_left_nether%`
  - `%wab_time_left_end%`
  - `%wab_time_left_elytra%`
- Per-feature and per-custom-world fallback spawn configuration (`fallback-spawns`).
- Unit tests for core restriction logic (`ConfigManagerTest`).

### Changed

- `/wab bypass` and `/wab remove` now support offline players and UUIDs.
- Bypass expiration timestamps now use the configured plugin timezone.
- Bypass persistence optimized with delayed save scheduling to reduce disk writes.
- Updated README with new commands, placeholders, and config examples.

### Fixed

- Fixed recurring behavior: `periods: []` now correctly means "always blocked".
- Fixed date parsing with configured timezone for `restriction-date`.
- Fixed edge cases and null-safety in restriction logic.
- Fixed potential crash in portal creation handling when block list is empty.
- Fixed teleport fallback handling when blocking Nether/End/custom world access.
- Fixed broken README encoding.

### Notes

- PlaceholderAPI integration is optional and auto-enabled when the plugin is installed.

---

## [0.7.x and earlier]

Initial releases — date-based Nether, End, and elytra restrictions with recurring schedule support.
