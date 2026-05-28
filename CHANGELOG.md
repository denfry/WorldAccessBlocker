# Changelog

All notable changes to WorldAccessBlocker are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [0.8.0] — 2026-05-28

### Added

- **Version checker** — on startup the plugin asynchronously queries the Modrinth API and notifies operators with the `wab.update-notify` permission when a newer release is available. Notification appears once per join session.
- **`wab.update-notify` permission** — dedicated permission for update notifications, separate from `wab.reload`. Default: op.
- **Folia-native scheduler** (`PlatformRuntime`) — new `runtime/` abstraction layer dispatches tasks through the Folia regional scheduler on Folia servers and the standard Bukkit scheduler elsewhere. Eliminates cross-thread teleport warnings on Folia.
- **`WorldEntryBlocker` listener** — handles `PlayerTeleportEvent` and `PlayerChangedWorldEvent` to block custom-world entry immediately on teleport, in addition to the periodic enforcer sweep.
- **CI/CD — build workflow** (`.github/workflows/build.yml`) — builds and runs tests automatically on every push and pull request to `master`.
- **CI/CD — release workflow** (`.github/workflows/release.yml`) — on `v*` tag push: builds the JAR, creates a GitHub Release (with tag message as changelog), and uploads the release to Modrinth automatically.

### Changed

- `VersionChecker` and `RestrictionEnforcer` now use `PlatformRuntime` instead of calling `Bukkit.getScheduler()` directly — improves Folia compatibility.
- Folia detection moved to `PlatformRuntimeFactory.isFoliaRuntime()` using class-presence check instead of version string parsing.
- Log message on Folia detection changed from a warning to an info message.

### Fixed

- `VersionChecker`: `latestVersion` field made `private volatile` — closes accidental external mutation path.
- `VersionChecker`: error stream is now properly drained and closed on non-200 HTTP responses, preventing socket leaks.
- `VersionChecker`: `parseSemver` strips pre-release labels (e.g. `-SNAPSHOT`, `-beta.1`) before integer parsing — avoids silent `null` on dev builds.

---

## [0.7.1] — 2025-05-XX

### Fixed

- Restriction logic edge cases in `RestrictionEnforcer`.
- README encoding normalization.

---

## [0.7.0] — 2025-05-XX

### Added

- Recurring schedule support (`recurring.periods` in config).
- `/wab status` command.
- WabPlaceholders (PlaceholderAPI integration).
- bStats metrics.

### Changed

- Config format extended with `recurring` block under each feature.

---

## [0.6.x and earlier]

Initial releases — date-based Nether, End, and elytra restrictions.
