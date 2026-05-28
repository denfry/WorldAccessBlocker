# Design: Version Checker, CI/CD, Modrinth Auto-Upload

**Date:** 2026-05-28
**Project:** WorldAccessBlocker (Paper plugin, v0.7.1, Java 21, Maven)

---

## 1. Scope

Three independent features shipped together:

1. **Version Checker** — notify admins in-game when a newer plugin version is available on Modrinth.
2. **CI/CD build workflow** — build and test on every push/PR.
3. **CI/CD release workflow** — build and auto-upload to Modrinth on version tag push.

---

## 2. Version Checker

### Goal

When an admin joins the server, show a chat message if a newer version of WorldAccessBlocker is available on Modrinth.

### Components

**`utils/VersionChecker.java`**

- Called once from `WorldAccessBlocker.onEnable()` via an async Bukkit task.
- Makes an HTTP GET to:
  ```
  https://api.modrinth.com/v2/project/worldaccessblocker/version?loaders=["paper"]&game_versions=["1.21.4"]&limit=1
  ```
  (Query filters by `paper` only to get the latest canonical release; the upload step targets all loaders.)
- Makes the request with `java.net.HttpURLConnection` — no new dependencies.
- JSON parsed manually (no Gson/Jackson): finds `"version_number"` key in the response string.
- Extracts `version_number` from the first array element.
- Compares against `plugin.getDescription().getVersion()` using string equality (both follow semver `X.Y.Z`).
- Stores result in `volatile String latestVersion` (written async, read on main thread in join event):
  - `null` — check not yet complete (or failed silently).
  - `""` — plugin is up to date.
  - `"X.Y.Z"` — newer version available.
- On network/parse error: logs a single warning, sets `latestVersion = ""` (no noise for admins).

**`listeners/UpdateNotifier.java`**

- Listens to `PlayerJoinEvent` (lowest priority, async-safe read of cached field).
- Condition: `latestVersion` is non-null and non-empty AND player has permission `wab.reload` (already OP-default).
- Sends message using `LanguageManager.getMessage("update_available", latestVersion)` — supports ru/en.
- Message is sent once per join (no repeat cooldown needed; field is set once at startup).

**Registration in `WorldAccessBlocker.onEnable()`**

```java
VersionChecker versionChecker = new VersionChecker(this);
versionChecker.checkAsync();
getServer().getPluginManager().registerEvents(new UpdateNotifier(this, versionChecker), this);
```

### Language keys

`en.yml`:
```yaml
update_available: "§e[WAB] §fA new version §a%s §fis available on Modrinth!"
```

`ru.yml`:
```yaml
update_available: "§e[WAB] §fДоступна новая версия §a%s §fна Modrinth!"
```

### Error handling

- Timeout: 5 seconds on connect + read.
- Any `IOException` or unexpected response: warn in server log once, set `latestVersion = ""`.
- Does not block server startup (async task).

---

## 3. CI/CD — Build Workflow

**File:** `.github/workflows/build.yml`

**Trigger:** `push` to `master`, `pull_request` targeting `master`.

**Steps:**
1. `actions/checkout@v4`
2. `actions/setup-java@v4` — Java 21, distribution `temurin`, Maven cache enabled.
3. `mvn clean package` — compiles, runs tests, produces shaded JAR.
4. `actions/upload-artifact@v4` — uploads `target/worldaccessblocker-*.jar` (excludes `*-original.jar`).

**Purpose:** Confirm every commit builds and all tests pass. JAR artifact available for manual download.

---

## 4. CI/CD — Release Workflow

**File:** `.github/workflows/release.yml`

**Trigger:** `push` tag matching `v*` (e.g., `v0.8.0`).

**Steps:**

1. `actions/checkout@v4` (with `fetch-depth: 0` to get tag message).
2. `actions/setup-java@v4` — Java 21, temurin, Maven cache.
3. `mvn clean package -DskipTests` — produces shaded JAR.
4. **Extract version** from tag: `VERSION=${GITHUB_REF_NAME#v}` → e.g., `0.8.0`.
5. **Extract changelog** from git tag message: `git tag -l --format='%(contents)' $GITHUB_REF_NAME`.
6. **Create GitHub Release** via `softprops/action-gh-release@v2`:
   - tag name from `GITHUB_REF_NAME`
   - body = tag message (changelog)
   - attach JAR file
7. **Upload to Modrinth** via `curl POST https://api.modrinth.com/v2/version`:
   - `Authorization: ${{ secrets.MODRINTH_TOKEN }}`
   - multipart form: `data` (JSON metadata) + `file` (JAR binary)
   - JSON metadata:
     ```json
     {
       "project_id": "worldaccessblocker",
       "version_number": "<VERSION>",
       "name": "WorldAccessBlocker <VERSION>",
       "changelog": "<TAG_MESSAGE>",
       "dependencies": [],
       "game_versions": ["1.21.4"],
       "version_type": "release",
       "loaders": ["paper", "spigot", "bukkit", "folia"],
       "featured": true,
       "file_parts": ["file"]
     }
     ```

**Required GitHub Secret:** `MODRINTH_TOKEN` — Modrinth personal access token with `CREATE_VERSION` + `MANAGE_VERSIONS` scopes.

**Permissions block in workflow:**
```yaml
permissions:
  contents: write
```
Required for creating GitHub Releases.

---

## 5. Files Changed / Created

| File | Action |
|---|---|
| `src/main/java/.../utils/VersionChecker.java` | Create |
| `src/main/java/.../listeners/UpdateNotifier.java` | Create |
| `src/main/java/.../WorldAccessBlocker.java` | Edit — wire VersionChecker + UpdateNotifier |
| `src/main/resources/lang/en.yml` | Edit — add `update_available` key |
| `src/main/resources/lang/ru.yml` | Edit — add `update_available` key |
| `.github/workflows/build.yml` | Create |
| `.github/workflows/release.yml` | Create |

---

## 6. Out of Scope

- Checking versions for dependencies (PlaceholderAPI, bStats).
- Sending update notifications to players without `wab.reload`.
- Supporting Modrinth channels other than `release`.
- Automatic `pom.xml` version bump on release.
