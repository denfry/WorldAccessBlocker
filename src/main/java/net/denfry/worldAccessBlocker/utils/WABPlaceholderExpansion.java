package net.denfry.worldAccessBlocker.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

public class WABPlaceholderExpansion extends PlaceholderExpansion {
    private final WorldAccessBlocker plugin;

    public WABPlaceholderExpansion(WorldAccessBlocker plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "wab";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        ConfigManager cfg = plugin.getConfigManager();
        Instant now = Instant.now();
        String key = params.toLowerCase();
        switch (key) {
            case "status_nether":
            case "status_end":
            case "status_elytra": {
                String feature = key.replace("status_", "");
                return getStatus(cfg, feature, now);
            }
            case "timeleft_nether":
            case "timeleft_end":
            case "timeleft_elytra": {
                String feature = key.replace("timeleft_", "");
                return getTimeLeft(cfg, feature, now);
            }
            default:
                if (key.startsWith("status_")) {
                    String feature = key.substring("status_".length());
                    return getStatus(cfg, feature, now);
                }
                if (key.startsWith("timeleft_")) {
                    String feature = key.substring("timeleft_".length());
                    return getTimeLeft(cfg, feature, now);
                }
                if (key.startsWith("schedule_")) {
                    String feature = key.substring("schedule_".length());
                    return cfg.getFormattedSchedule(feature);
                }
        }
        return "";
    }

    private String getStatus(ConfigManager cfg, String feature, Instant now) {
        if (!cfg.getDisable(feature)) return "open";
        if (!cfg.isRestrictionActive(feature, now)) return "open";
        if (cfg.isRecurring(feature)) {
            return "blocked (schedule: " + cfg.getFormattedSchedule(feature) + ")";
        }
        return "blocked";
    }

    private String getTimeLeft(ConfigManager cfg, String feature, Instant now) {
        if (!cfg.isRestrictionActive(feature, now)) return "0";
        Duration d = Duration.between(now, cfg.getRestrictionInstant(feature));
        if (d.isNegative() || d.isZero()) return "0";
        long days = d.toDays();
        long hours = d.toHours() % 24;
        long minutes = d.toMinutes() % 60;
        return days + "d " + hours + "h " + minutes + "m";
    }
}

