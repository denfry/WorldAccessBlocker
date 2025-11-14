package net.denfry.worldAccessBlocker;

import net.denfry.worldAccessBlocker.listeners.ElytraBlocker;
import net.denfry.worldAccessBlocker.listeners.EndBlocker;
import net.denfry.worldAccessBlocker.listeners.PortalBlocker;
import net.denfry.worldAccessBlocker.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.logging.Logger;

public class WorldAccessBlocker extends JavaPlugin {
    private final Logger log = getLogger();
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private BypassManager bypassManager;

    @Override
    public void onEnable() {
        log.info("⚙️ Initializing WorldAccessBlocker...");

        configManager = new ConfigManager(this);
        languageManager = new LanguageManager(this);
        bypassManager = new BypassManager(this);
        configManager.loadConfigValues();
        bypassManager.loadBypasses();

        log.info("📂 Configuration loaded:");
        log.info("  ├─ 🔥 Nether restriction: " + configManager.isDisableNether() + " (until " + configManager.getNetherRestrictionInstant() + ")");
        log.info("  ├─ ⚫ End restriction: " + configManager.isDisableEnd() + " (until " + configManager.getEndRestrictionInstant() + ")");
        log.info("  ├─ 🛑 Elytra restriction: " + configManager.isDisableElytra() + " (until " + configManager.getElytraRestrictionInstant() + ")");
        log.info("  ├─ 🌐 Language: " + getConfig().getString("language", "en"));
        log.info("  └─ 🗺️ Custom worlds restrictions:");
        for (String world : configManager.getCustomWorlds()) {
            log.info("     - " + world + ": " + configManager.isCustomWorldDisabled(world) + " (until " + configManager.getCustomWorldRestrictionInstant(world) + ")");
        }

        log.info("🔗 Registering events...");
        getServer().getPluginManager().registerEvents(new PortalBlocker(this), this);
        getServer().getPluginManager().registerEvents(new EndBlocker(this), this);
        getServer().getPluginManager().registerEvents(new ElytraBlocker(this), this);
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

    public Location getOverworldSpawn() {
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
        if (recurring) {
            String schedule = configManager.getFormattedSchedule(feature);
            Object[] args = baseKey.contains("custom") ? new Object[]{feature, schedule} : new Object[]{schedule};
            text = languageManager.getMessage(key, args);
        } else {
            Instant until = configManager.getRestrictionInstant(feature);
            Duration d = Duration.between(Instant.now(), until);
            if (d.isNegative() || d.isZero()) return;

            long days = d.toDays();
            long hours = d.toHours() % 24;
            Object[] args = baseKey.contains("custom") ? new Object[]{feature, days, hours} : new Object[]{days, hours};
            text = languageManager.getMessage(key, args);
        }

        player.sendMessage(Component.text(text).color(NamedTextColor.RED));
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
}