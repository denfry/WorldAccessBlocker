# Version Checker, CI/CD, Modrinth Auto-Upload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Notify admins in-game when a new plugin version is available on Modrinth, add a GitHub Actions build workflow for all pushes, and auto-upload JAR to Modrinth on version tag push.

**Architecture:** A `VersionChecker` utility class performs a one-time async HTTP check against Modrinth API on startup, caches the result in a `volatile` field, and an `UpdateNotifier` event listener reads that cache on every admin join. Two GitHub Actions workflows handle CI (build + test on push) and CD (build + GitHub Release + Modrinth upload on `v*` tag).

**Tech Stack:** Java 21, Paper API 1.21.4, `java.net.HttpURLConnection` (no new deps), JUnit 5 + Mockito (existing), GitHub Actions, Modrinth API v2, `curl` + `jq`.

---

## File Map

| File | Action |
|---|---|
| `src/main/java/net/denfry/worldAccessBlocker/utils/VersionChecker.java` | Create |
| `src/main/java/net/denfry/worldAccessBlocker/listeners/UpdateNotifier.java` | Create |
| `src/main/java/net/denfry/worldAccessBlocker/WorldAccessBlocker.java` | Modify — wire VersionChecker + UpdateNotifier in `onEnable` |
| `src/main/resources/lang/en.yml` | Modify — add `update_available` key |
| `src/main/resources/lang/ru.yml` | Modify — add `update_available` key |
| `src/test/java/net/denfry/worldAccessBlocker/utils/VersionCheckerTest.java` | Create |
| `src/test/java/net/denfry/worldAccessBlocker/listeners/UpdateNotifierTest.java` | Create |
| `.github/workflows/build.yml` | Create |
| `.github/workflows/release.yml` | Create |

---

## Task 1: Add language keys for update notification

**Files:**
- Modify: `src/main/resources/lang/en.yml`
- Modify: `src/main/resources/lang/ru.yml`

- [ ] **Step 1: Add key to en.yml**

Open `src/main/resources/lang/en.yml` and append at the end:

```yaml
update_available: "§e[WAB] §fA new version §a%s §fis available! https://modrinth.com/plugin/worldaccessblocker"
```

- [ ] **Step 2: Add key to ru.yml**

Open `src/main/resources/lang/ru.yml` and append at the end:

```yaml
update_available: "§e[WAB] §fДоступна новая версия §a%s§f! https://modrinth.com/plugin/worldaccessblocker"
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/lang/en.yml src/main/resources/lang/ru.yml
git commit -m "feat: add update_available language key"
```

---

## Task 2: VersionChecker — parsing and version comparison (TDD)

**Files:**
- Create: `src/main/java/net/denfry/worldAccessBlocker/utils/VersionChecker.java`
- Create: `src/test/java/net/denfry/worldAccessBlocker/utils/VersionCheckerTest.java`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/net/denfry/worldAccessBlocker/utils/VersionCheckerTest.java`:

```java
package net.denfry.worldAccessBlocker.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionCheckerTest {

    // --- parseVersionFromResponse ---

    @Test
    void parsesVersionFromTypicalModrinthResponse() {
        String json = "[{\"id\":\"abc\",\"project_id\":\"worldaccessblocker\",\"version_number\":\"0.8.0\",\"name\":\"WorldAccessBlocker 0.8.0\"}]";
        assertEquals("0.8.0", VersionChecker.parseVersionFromResponse(json));
    }

    @Test
    void returnsNullForEmptyArray() {
        assertNull(VersionChecker.parseVersionFromResponse("[]"));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(VersionChecker.parseVersionFromResponse(null));
    }

    @Test
    void returnsNullForBlankInput() {
        assertNull(VersionChecker.parseVersionFromResponse("   "));
    }

    @Test
    void returnsNullWhenFieldMissing() {
        assertNull(VersionChecker.parseVersionFromResponse("[{\"id\":\"abc\"}]"));
    }

    // --- isNewer ---

    @Test
    void detectsNewerMinorVersion() {
        assertTrue(VersionChecker.isNewer("0.8.0", "0.7.1"));
    }

    @Test
    void detectsNewerPatchVersion() {
        assertTrue(VersionChecker.isNewer("0.7.2", "0.7.1"));
    }

    @Test
    void detectsNewerMajorVersion() {
        assertTrue(VersionChecker.isNewer("1.0.0", "0.7.1"));
    }

    @Test
    void returnsFalseForSameVersion() {
        assertFalse(VersionChecker.isNewer("0.7.1", "0.7.1"));
    }

    @Test
    void returnsFalseWhenCandidateIsOlder() {
        assertFalse(VersionChecker.isNewer("0.7.0", "0.7.1"));
    }

    @Test
    void returnsFalseForMalformedVersion() {
        assertFalse(VersionChecker.isNewer("nightly", "0.7.1"));
        assertFalse(VersionChecker.isNewer("0.8.0", "invalid"));
    }
}
```

- [ ] **Step 2: Run tests — expect compilation failure**

```bash
mvn test -Dtest=VersionCheckerTest -pl . 2>&1 | tail -20
```

Expected: `COMPILATION ERROR` — `VersionChecker` does not exist yet.

- [ ] **Step 3: Create VersionChecker with the two static methods**

Create `src/main/java/net/denfry/worldAccessBlocker/utils/VersionChecker.java`:

```java
package net.denfry.worldAccessBlocker.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VersionChecker {

    private static final String API_URL =
            "https://api.modrinth.com/v2/project/worldaccessblocker/version?limit=1";

    private final JavaPlugin plugin;
    private final String currentVersion;
    volatile String latestVersion = null; // null=pending, ""=up-to-date, "x.y.z"=new version

    public VersionChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void checkAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String response = fetchResponse();
                String latest = parseVersionFromResponse(response);
                if (latest != null && isNewer(latest, currentVersion)) {
                    latestVersion = latest;
                } else {
                    latestVersion = "";
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[WAB] Version check failed: " + e.getMessage());
                latestVersion = "";
            }
        });
    }

    String fetchResponse() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("User-Agent",
                "WorldAccessBlocker/" + currentVersion + " (github.com/denfry)");
        try {
            if (conn.getResponseCode() != 200) return "[]";
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    static String parseVersionFromResponse(String json) {
        if (json == null || json.isBlank()) return null;
        String trimmed = json.trim();
        if (trimmed.equals("[]")) return null;
        int idx = trimmed.indexOf("\"version_number\"");
        if (idx < 0) return null;
        int colon = trimmed.indexOf(":", idx);
        if (colon < 0) return null;
        int start = trimmed.indexOf("\"", colon);
        if (start < 0) return null;
        start++;
        int end = trimmed.indexOf("\"", start);
        if (end < 0) return null;
        return trimmed.substring(start, end);
    }

    static boolean isNewer(String candidate, String current) {
        int[] c = parseSemver(candidate);
        int[] cur = parseSemver(current);
        if (c == null || cur == null) return false;
        for (int i = 0; i < 3; i++) {
            if (c[i] > cur[i]) return true;
            if (c[i] < cur[i]) return false;
        }
        return false;
    }

    private static int[] parseSemver(String v) {
        if (v == null) return null;
        String[] parts = v.split("\\.", 3);
        if (parts.length < 2) return null;
        try {
            int[] result = new int[3];
            result[0] = Integer.parseInt(parts[0]);
            result[1] = Integer.parseInt(parts[1]);
            result[2] = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return result;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
```

- [ ] **Step 4: Run tests — expect all pass**

```bash
mvn test -Dtest=VersionCheckerTest -pl .
```

Expected output:
```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/denfry/worldAccessBlocker/utils/VersionChecker.java \
        src/test/java/net/denfry/worldAccessBlocker/utils/VersionCheckerTest.java
git commit -m "feat: add VersionChecker with Modrinth API integration"
```

---

## Task 3: UpdateNotifier — admin join listener (TDD)

**Files:**
- Create: `src/main/java/net/denfry/worldAccessBlocker/listeners/UpdateNotifier.java`
- Create: `src/test/java/net/denfry/worldAccessBlocker/listeners/UpdateNotifierTest.java`

- [ ] **Step 1: Write the failing tests**

Create directory `src/test/java/net/denfry/worldAccessBlocker/listeners/` if it does not exist, then create `UpdateNotifierTest.java`:

```java
package net.denfry.worldAccessBlocker.listeners;

import net.denfry.worldAccessBlocker.utils.LanguageManager;
import net.denfry.worldAccessBlocker.utils.VersionChecker;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateNotifierTest {

    private Player player;
    private PlayerJoinEvent event;
    private VersionChecker versionChecker;
    private LanguageManager languageManager;
    private UpdateNotifier notifier;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);

        versionChecker = mock(VersionChecker.class);
        languageManager = mock(LanguageManager.class);
        when(languageManager.getMessage(eq("update_available"), any()))
                .thenReturn("[WAB] New version 0.8.0 available!");

        notifier = new UpdateNotifier(versionChecker, languageManager);
    }

    @Test
    void sendsMessageWhenNewVersionAndHasPermission() {
        when(versionChecker.getLatestVersion()).thenReturn("0.8.0");
        when(player.hasPermission("wab.reload")).thenReturn(true);

        notifier.onPlayerJoin(event);

        verify(player).sendMessage(any(Component.class));
    }

    @Test
    void doesNotSendWhenNoPermission() {
        when(versionChecker.getLatestVersion()).thenReturn("0.8.0");
        when(player.hasPermission("wab.reload")).thenReturn(false);

        notifier.onPlayerJoin(event);

        verify(player, never()).sendMessage(any(Component.class));
    }

    @Test
    void doesNotSendWhenUpToDate() {
        when(versionChecker.getLatestVersion()).thenReturn("");
        when(player.hasPermission("wab.reload")).thenReturn(true);

        notifier.onPlayerJoin(event);

        verify(player, never()).sendMessage(any(Component.class));
    }

    @Test
    void doesNotSendWhenCheckPending() {
        when(versionChecker.getLatestVersion()).thenReturn(null);
        when(player.hasPermission("wab.reload")).thenReturn(true);

        notifier.onPlayerJoin(event);

        verify(player, never()).sendMessage(any(Component.class));
    }
}
```

- [ ] **Step 2: Run tests — expect compilation failure**

```bash
mvn test -Dtest=UpdateNotifierTest -pl . 2>&1 | tail -20
```

Expected: `COMPILATION ERROR` — `UpdateNotifier` does not exist.

- [ ] **Step 3: Create UpdateNotifier**

Create `src/main/java/net/denfry/worldAccessBlocker/listeners/UpdateNotifier.java`:

```java
package net.denfry.worldAccessBlocker.listeners;

import net.denfry.worldAccessBlocker.utils.LanguageManager;
import net.denfry.worldAccessBlocker.utils.VersionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateNotifier implements Listener {

    private final VersionChecker versionChecker;
    private final LanguageManager languageManager;

    public UpdateNotifier(VersionChecker versionChecker, LanguageManager languageManager) {
        this.versionChecker = versionChecker;
        this.languageManager = languageManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String latest = versionChecker.getLatestVersion();
        if (latest == null || latest.isEmpty()) return;
        if (!event.getPlayer().hasPermission("wab.reload")) return;
        String text = languageManager.getMessage("update_available", latest);
        event.getPlayer().sendMessage(Component.text(text).color(NamedTextColor.YELLOW));
    }
}
```

- [ ] **Step 4: Run tests — expect all pass**

```bash
mvn test -Dtest=UpdateNotifierTest -pl .
```

Expected output:
```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/denfry/worldAccessBlocker/listeners/UpdateNotifier.java \
        src/test/java/net/denfry/worldAccessBlocker/listeners/UpdateNotifierTest.java
git commit -m "feat: add UpdateNotifier listener for admin update alerts"
```

---

## Task 4: Wire VersionChecker and UpdateNotifier into WorldAccessBlocker

**Files:**
- Modify: `src/main/java/net/denfry/worldAccessBlocker/WorldAccessBlocker.java`

- [ ] **Step 1: Add imports and field**

Open `WorldAccessBlocker.java`. Add two imports after the existing listener imports:

```java
import net.denfry.worldAccessBlocker.listeners.UpdateNotifier;
import net.denfry.worldAccessBlocker.utils.VersionChecker;
```

- [ ] **Step 2: Add wiring in onEnable**

In `onEnable()`, find the line:

```java
Bukkit.getScheduler().runTaskTimer(this, new RestrictionEnforcer(this), 0L, 100L);
log.info("WorldAccessBlocker enabled.");
```

Replace with:

```java
Bukkit.getScheduler().runTaskTimer(this, new RestrictionEnforcer(this), 0L, 100L);

VersionChecker versionChecker = new VersionChecker(this);
versionChecker.checkAsync();
getServer().getPluginManager().registerEvents(new UpdateNotifier(versionChecker, languageManager), this);

log.info("WorldAccessBlocker enabled.");
```

- [ ] **Step 3: Run full test suite**

```bash
mvn clean package
```

Expected output:
```
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

(11 VersionCheckerTest + 4 UpdateNotifierTest + 4 existing ConfigManagerTest = 19 total)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/net/denfry/worldAccessBlocker/WorldAccessBlocker.java
git commit -m "feat: wire version checker and update notifier on startup"
```

---

## Task 5: GitHub Actions — build workflow

**Files:**
- Create: `.github/workflows/build.yml`

- [ ] **Step 1: Create the workflows directory**

```bash
mkdir -p .github/workflows
```

- [ ] **Step 2: Create build.yml**

Create `.github/workflows/build.yml`:

```yaml
name: Build

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build and test
        run: mvn clean package

      - name: Upload JAR artifact
        uses: actions/upload-artifact@v4
        with:
          name: worldaccessblocker-snapshot
          path: target/worldaccessblocker-*.jar
          if-no-files-found: error
```

- [ ] **Step 3: Verify YAML syntax locally**

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml'))" && echo "YAML OK"
```

Expected: `YAML OK`

If python3 is not available, skip this step — GitHub will validate on push.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: add build workflow for push and pull requests"
```

---

## Task 6: GitHub Actions — release workflow (Modrinth + GitHub Release)

**Files:**
- Create: `.github/workflows/release.yml`

- [ ] **Step 1: Create release.yml**

Create `.github/workflows/release.yml`:

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: write

jobs:
  release:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build
        run: mvn clean package -DskipTests

      - name: Extract version from tag
        id: version
        run: echo "VERSION=${GITHUB_REF_NAME#v}" >> "$GITHUB_OUTPUT"

      - name: Extract changelog from tag message
        id: changelog
        run: |
          CHANGELOG=$(git tag -l --format='%(contents)' "$GITHUB_REF_NAME")
          {
            echo "CHANGELOG<<EOF"
            echo "$CHANGELOG"
            echo "EOF"
          } >> "$GITHUB_OUTPUT"

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          name: "WorldAccessBlocker ${{ steps.version.outputs.VERSION }}"
          body: ${{ steps.changelog.outputs.CHANGELOG }}
          files: target/worldaccessblocker-${{ steps.version.outputs.VERSION }}.jar

      - name: Upload to Modrinth
        env:
          MODRINTH_TOKEN: ${{ secrets.MODRINTH_TOKEN }}
          VERSION: ${{ steps.version.outputs.VERSION }}
          CHANGELOG: ${{ steps.changelog.outputs.CHANGELOG }}
        run: |
          JAR_PATH="target/worldaccessblocker-${VERSION}.jar"
          DATA=$(jq -n \
            --arg version "$VERSION" \
            --arg changelog "$CHANGELOG" \
            '{
              "project_id": "worldaccessblocker",
              "version_number": $version,
              "name": ("WorldAccessBlocker " + $version),
              "changelog": $changelog,
              "dependencies": [],
              "game_versions": ["1.21.4"],
              "version_type": "release",
              "loaders": ["paper", "spigot", "bukkit", "folia"],
              "featured": true,
              "file_parts": ["file"]
            }')
          curl --fail -s -X POST "https://api.modrinth.com/v2/version" \
            -H "Authorization: $MODRINTH_TOKEN" \
            -F "data=$DATA" \
            -F "file=@${JAR_PATH}"
```

- [ ] **Step 2: Verify YAML syntax**

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/release.yml'))" && echo "YAML OK"
```

Expected: `YAML OK`

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: add release workflow with GitHub Release and Modrinth upload"
```

---

## Task 7: Final verification

- [ ] **Step 1: Run the full build one more time**

```bash
mvn clean package
```

Expected:
```
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

(4 ConfigManagerTest + 11 VersionCheckerTest + 4 UpdateNotifierTest)

- [ ] **Step 2: Verify all expected files exist**

```bash
ls src/main/java/net/denfry/worldAccessBlocker/utils/VersionChecker.java
ls src/main/java/net/denfry/worldAccessBlocker/listeners/UpdateNotifier.java
ls .github/workflows/build.yml
ls .github/workflows/release.yml
```

Expected: all four lines print without error.

- [ ] **Step 3: Smoke-test the release workflow manually (optional)**

To test the full release pipeline without writing code, push a test tag:

```bash
git tag v0.7.2 -m "Test release: verify CI/CD pipeline end-to-end"
git push origin v0.7.2
```

Watch the **Actions** tab on GitHub. The `Release` workflow should:
1. Build the JAR
2. Create a GitHub Release named `WorldAccessBlocker 0.7.2`
3. Upload JAR to Modrinth

If you need to delete the test tag afterwards:
```bash
git tag -d v0.7.2
git push origin --delete v0.7.2
```

---

## How to release a real version

1. Update `pom.xml` version to the new number (e.g., `0.8.0`)
2. Commit: `git commit -am "chore: bump version to 0.8.0"`
3. Tag with changelog as the tag message:
   ```bash
   git tag v0.8.0 -m "## What's new
   - Added version checker
   - Added CI/CD pipeline
   - Added Modrinth auto-upload"
   ```
4. Push tag: `git push origin v0.8.0`
5. CI does the rest automatically.
