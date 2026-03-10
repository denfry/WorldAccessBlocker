package net.denfry.worldAccessBlocker.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

public class WabPlaceholders extends PlaceholderExpansion {
    private final WorldAccessBlocker plugin;

    public WabPlaceholders(WorldAccessBlocker plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "wab";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        return switch (params.toLowerCase()) {
            case "nether_blocked" -> booleanToString(isBlockedForPlayer(player, "nether"));
            case "end_blocked" -> booleanToString(isBlockedForPlayer(player, "end"));
            case "elytra_blocked" -> booleanToString(isBlockedForPlayer(player, "elytra"));
            case "time_left_nether" -> timeLeft("nether");
            case "time_left_end" -> timeLeft("end");
            case "time_left_elytra" -> timeLeft("elytra");
            default -> null;
        };
    }

    private boolean isBlockedForPlayer(Player player, String feature) {
        Instant now = Instant.now();
        return plugin.getConfigManager().isRestrictionActive(feature, now) && plugin.isRestricted(player, feature);
    }

    private String timeLeft(String feature) {
        Instant now = Instant.now();
        if (!plugin.getConfigManager().isRestrictionActive(feature, now)) {
            return "0d 0h";
        }
        if (plugin.getConfigManager().isRecurring(feature)) {
            return plugin.getConfigManager().getFormattedSchedule(feature);
        }

        Duration left = Duration.between(now, plugin.getConfigManager().getRestrictionInstant(feature));
        if (left.isNegative() || left.isZero()) return "0d 0h";
        return left.toDays() + "d " + (left.toHours() % 24) + "h";
    }

    private String booleanToString(boolean value) {
        return value ? "true" : "false";
    }
}
