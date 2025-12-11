package net.denfry.worldAccessBlocker;

import net.denfry.worldAccessBlocker.listeners.ElytraBlocker;
import net.denfry.worldAccessBlocker.listeners.EndBlocker;
import net.denfry.worldAccessBlocker.listeners.PortalBlocker;
import net.denfry.worldAccessBlocker.listeners.RegionBlockListener;
import net.denfry.worldAccessBlocker.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.logging.Logger;

public class WorldAccessBlocker extends JavaPlugin {
    private final Logger log = getLogger();
    private final MiniMessage mini = MiniMessage.miniMessage();
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private BypassManager bypassManager;
    private WorldGuardIntegration worldGuardIntegration;
    private DiscordNotifier discordNotifier;
    private BossBarManager bossBarManager;
    private StatsTracker statsTracker;
    private SyncManager syncManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        log.info("⚙️ Initializing WorldAccessBlocker...");

        configManager = new ConfigManager(this);
        languageManager = new LanguageManager(this);
        bypassManager = new BypassManager(this);
        discordNotifier = new DiscordNotifier(this);
        bossBarManager = new BossBarManager(this);
        statsTracker = new StatsTracker();
        syncManager = new SyncManager(this);
        configManager.loadConfigValues();
        bypassManager.loadBypasses();
        if (configManager.isAutoCleanBypasses()) {
            int cleaned = bypassManager.clearExpired();
            if (cleaned > 0) {
                log.info("🧹 Removed expired bypasses: " + cleaned);
            }
        }

        languageManager.startAutoReload();

        if (configManager.isProEdition() && configManager.isGuiEnabled()) {
            guiManager = new GuiManager(this);
            getServer().getPluginManager().registerEvents(guiManager, this);
        }

        log.info("📂 Configuration loaded:");
        log.info("  ├─ 🔥 Nether restriction: " + configManager.isDisableNether() + " (until " + configManager.getNetherRestrictionInstant() + ")");
        log.info("  ├─ ⚫ End restriction: " + configManager.isDisableEnd() + " (until " + configManager.getEndRestrictionInstant() + ")");
        log.info("  ├─ 🛑 Elytra restriction: " + configManager.isDisableElytra() + " (until " + configManager.getElytraRestrictionInstant() + ")");
        log.info("  ├─ 🌐 Language: " + getConfig().getString("language", "en"));
        log.info("  ├─ 🪪 Edition: " + (configManager.isProEdition() ? "PRO" : "FREE"));
        log.info("  └─ 🗺️ Custom worlds restrictions:");
        for (String world : configManager.getCustomWorlds()) {
            log.info("     - " + world + ": " + configManager.isCustomWorldDisabled(world) + " (until " + configManager.getCustomWorldRestrictionInstant(world) + ")");
        }

        log.info("🔗 Registering events...");
        getServer().getPluginManager().registerEvents(new PortalBlocker(this), this);
        getServer().getPluginManager().registerEvents(new EndBlocker(this), this);
        getServer().getPluginManager().registerEvents(new ElytraBlocker(this), this);
        if (configManager.isProEdition() && getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardIntegration = new WorldGuardIntegration(this);
            worldGuardIntegration.registerFlag();
            getServer().getPluginManager().registerEvents(new RegionBlockListener(this, worldGuardIntegration), this);
            log.info("🛡️ WorldGuard integration enabled.");
        }
        log.info("✅ All event handlers registered!");

        if (getCommand("wabreload") != null) {
            Objects.requireNonNull(getCommand("wabreload")).setExecutor(new ReloadCommand(this, configManager, languageManager));
            log.info("📝 Command /wabreload successfully registered!");
        } else {
            log.severe("❌ Error: Command /wabreload not registered in plugin.yml!");
        }

        if (getCommand("wab") != null) {
            BypassCommand bypassCommand = new BypassCommand(this, bypassManager);
            Objects.requireNonNull(getCommand("wab")).setExecutor(bypassCommand);
            Objects.requireNonNull(getCommand("wab")).setTabCompleter(bypassCommand);
            log.info("📝 Command /wab successfully registered!");
        } else {
            log.severe("❌ Error: Command /wab not registered in plugin.yml!");
        }

        if (Bukkit.getVersion().contains("Folia")) {
            log.warning("⚠️ WARNING: Server is running on Folia!");
            log.warning("⚠️ Portal cancellation via PlayerPortalEvent does NOT work in Folia.");
            log.warning("⚠️ Issues may occur with Nether access blocking.");
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new WABPlaceholderExpansion(this).register();
            log.info("📎 PlaceholderAPI hook enabled.");
        }

        syncManager.register();

        try {
            int pluginId = 26810;
            Metrics metrics = new Metrics(this, pluginId);
            metrics.addCustomChart(new SimplePie("nether_disabled", () -> configManager.isDisableNether() ? "Enabled" : "Disabled"));
            metrics.addCustomChart(new SimplePie("end_disabled", () -> configManager.isDisableEnd() ? "Enabled" : "Disabled"));
            metrics.addCustomChart(new SimplePie("elytra_disabled", () -> configManager.isDisableElytra() ? "Enabled" : "Disabled"));
            log.info("📊 bStats metrics successfully initialized!");
        } catch (Exception e) {
            log.warning("⚠️ Failed to initialize bStats metrics: " + e.getMessage());
        }

        scheduleRestrictionCheck();
        scheduleUpdateCheck();
        log.info("✅ WorldAccessBlocker plugin successfully loaded!");
    }

    @Override
    public void onDisable() {
        bypassManager.saveBypasses();
        log.info("💾 Bypasses saved successfully!");
    }

    private void scheduleRestrictionCheck() {
        Bukkit.getScheduler().runTaskTimer(this, new RestrictionEnforcer(this), 0L, 100L);
    }

    private void scheduleUpdateCheck() {
        if (!configManager.isUpdateCheckEnabled()) return;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String project = configManager.getModrinthId();
                String url = "https://api.modrinth.com/v2/project/" + project + "/version";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", "WorldAccessBlocker/" + getPluginMeta().getVersion())
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) return;

                String body = response.body();
                int idx = body.indexOf("\"version_number\"");
                if (idx == -1) return;
                int start = body.indexOf('"', idx + 17);
                int end = body.indexOf('"', start + 1);
                if (start == -1 || end == -1) return;
                String latest = body.substring(start + 1, end);
                String current = getPluginMeta().getVersion();
                if (!latest.equalsIgnoreCase(current)) {
                    String download = "https://modrinth.com/plugin/" + project;
                    String text = languageManager.getMessage("update_available", current, latest, download);
                    Component message = mini.deserialize(text).colorIfAbsent(NamedTextColor.YELLOW);
                    log.info(text);
                    Bukkit.getOnlinePlayers().stream()
                            .filter(p -> p.isOp() || p.hasPermission("wab.status"))
                            .forEach(p -> p.sendMessage(message));
                    if (configManager.isProEdition() && configManager.isDiscordEnabled() && configManager.isDiscordSendUpdate()) {
                        getDiscordNotifier().send(languageManager.getMessage("discord_update", current, latest));
                    }
                }
            } catch (Exception e) {
                log.fine("Update check failed: " + e.getMessage());
            }
        });
    }

    public Location getOverworldSpawn() {
        Location custom = configManager.getCustomSpawnLocation();
        if (custom != null) return custom;
        return Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .map(World::getSpawnLocation)
                .orElse(Bukkit.getWorlds().getFirst().getSpawnLocation());
    }

    public boolean isRestricted(Player player, String feature) {
        return bypassManager.isRestricted(player.getUniqueId(), feature);
    }

    public void sendRestrictionMessage(Player player, String feature) {
        boolean recurring = configManager.isRecurring(feature);
        String baseKey = switch (feature) {
            case "elytra" -> "elytra_blocked";
            case "elytra_equip" -> "elytra_equip_blocked";
            case "nether" -> "nether_blocked";
            case "end" -> "end_blocked";
            default -> "custom_world_blocked";
        };

        String key = baseKey + (recurring ? "_recurring" : "");

        String text;
        Instant until = configManager.getRestrictionInstant(feature);
        if (recurring) {
            String schedule = configManager.getFormattedSchedule(feature);
            Object[] args = baseKey.contains("custom") ? new Object[]{feature, schedule} : new Object[]{schedule};
            text = languageManager.getMessage(key, args);
        } else {
            Duration d = Duration.between(Instant.now(), until);
            if (d.isNegative() || d.isZero()) return;

            long days = d.toDays();
            long hours = d.toHours() % 24;
            Object[] args = baseKey.contains("custom") ? new Object[]{feature, days, hours} : new Object[]{days, hours};
            text = languageManager.getMessage(key, args);
        }

        Component message = (text.contains("§")
                ? LegacyComponentSerializer.legacySection().deserialize(text)
                : mini.deserialize(text)).colorIfAbsent(NamedTextColor.RED);
        String mode = configManager.getNotificationMode();
        if ("actionbar".equalsIgnoreCase(mode) || "both".equalsIgnoreCase(mode)) {
            player.sendActionBar(message);
        }
        if ("chat".equalsIgnoreCase(mode) || "both".equalsIgnoreCase(mode)) {
            player.sendMessage(message);
        }

        String sound = configManager.getNotificationSound();
        if (sound != null && !sound.isBlank()) {
            Sound resolved = Registry.SOUNDS.get(NamespacedKey.minecraft(sound.toLowerCase(Locale.ROOT)));
            if (resolved != null) {
                player.playSound(player.getLocation(), resolved, 1f, 1f);
            }
        }

        if (configManager.isAdminAlert()) {
            String alertText = languageManager.getMessage("admin_alert", player.getName(), feature);
            Component alert = mini.deserialize(alertText).colorIfAbsent(NamedTextColor.GRAY);
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.isOp() || p.hasPermission("wab.status"))
                    .forEach(p -> p.sendMessage(alert));
        }

        if (configManager.isProEdition() && configManager.isNotificationBossbar()) {
            Duration d = Duration.between(Instant.now(), until);
            getBossBarManager().show(player, feature, d, message);
        }

        if (configManager.isProEdition() && configManager.isStatsEnabled()) {
            getStatsTracker().increment(feature);
        }

        if (configManager.isProEdition() && configManager.isDiscordEnabled() && configManager.isDiscordSendBlock()) {
            String payload = languageManager.getMessage("discord_block", player.getName(), player.getWorld().getName(), feature);
            getDiscordNotifier().send(payload);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public BypassManager getBypassManager() {
        return bypassManager;
    }

    public DiscordNotifier getDiscordNotifier() {
        return discordNotifier;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public StatsTracker getStatsTracker() {
        return statsTracker;
    }

    public SyncManager getSyncManager() {
        return syncManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}
