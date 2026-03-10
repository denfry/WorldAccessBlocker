package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class BypassCommand implements CommandExecutor, TabCompleter {
    private final WorldAccessBlocker plugin;
    private final BypassManager bypassManager;

    public BypassCommand(WorldAccessBlocker plugin, BypassManager bypassManager) {
        this.plugin = plugin;
        this.bypassManager = bypassManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("wab.bypass")) {
            sendLocalized(sender, NamedTextColor.RED, "no_permission");
            return true;
        }

        if (args.length < 1) {
            sendLocalized(sender, NamedTextColor.YELLOW, "wab_usage");
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "bypass" -> handleBypass(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "status" -> handleStatus(sender, args);
            default -> {
                sendLocalized(sender, NamedTextColor.YELLOW, "wab_usage");
                yield true;
            }
        };
    }

    private boolean handleBypass(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendLocalized(sender, NamedTextColor.YELLOW, "wab_bypass_usage");
            return true;
        }

        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            sendLocalized(sender, NamedTextColor.RED, "wab_player_not_found", args[1]);
            return true;
        }

        String feature = args[2].toLowerCase();
        if (!isFeatureValid(feature)) {
            sendLocalized(sender, NamedTextColor.RED, "wab_invalid_feature", args[2]);
            return true;
        }

        long seconds;
        try {
            seconds = Long.parseLong(args[3]);
            if (seconds <= 0) throw new NumberFormatException("duration must be > 0");
        } catch (NumberFormatException e) {
            sendLocalized(sender, NamedTextColor.RED, "wab_invalid_duration");
            return true;
        }

        Instant bypassUntil = Instant.now().plusSeconds(seconds);
        bypassManager.grantBypass(target.getUniqueId(), feature, bypassUntil);
        sendLocalized(sender, NamedTextColor.GREEN, "bypass_granted", feature, target.getName(), formatInstant(bypassUntil));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendLocalized(sender, NamedTextColor.YELLOW, "wab_remove_usage");
            return true;
        }

        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            sendLocalized(sender, NamedTextColor.RED, "wab_player_not_found", args[1]);
            return true;
        }

        String feature = args[2].toLowerCase();
        if (!isFeatureValid(feature)) {
            sendLocalized(sender, NamedTextColor.RED, "wab_invalid_feature", args[2]);
            return true;
        }

        if (bypassManager.removeBypass(target.getUniqueId(), feature)) {
            sendLocalized(sender, NamedTextColor.GREEN, "bypass_removed", feature, target.getName());
        } else {
            sendLocalized(sender, NamedTextColor.YELLOW, "wab_no_active_bypass", feature, target.getName());
        }

        return true;
    }

    private boolean handleStatus(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) {
            target = resolvePlayer(args[1]);
            if (target == null || target.getUniqueId() == null) {
                sendLocalized(sender, NamedTextColor.RED, "wab_player_not_found", args[1]);
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sendLocalized(sender, NamedTextColor.YELLOW, "wab_status_usage");
            return true;
        }

        Instant now = Instant.now();
        String playerName = target.getName() == null ? target.getUniqueId().toString() : target.getName();
        sendLocalized(sender, NamedTextColor.AQUA, "wab_status_header", playerName);

        for (String feature : getAllFeatures()) {
            boolean globallyBlocked = plugin.getConfigManager().isRestrictionActive(feature, now);
            Instant bypassUntil = bypassManager.getActiveBypassUntil(target.getUniqueId(), feature, now);
            boolean blockedForPlayer = globallyBlocked && bypassUntil == null;
            String detail = buildDetail(feature, now, bypassUntil);

            String statusText = plugin.getLanguageManager().getMessage(
                    "wab_status_line",
                    feature,
                    blockedForPlayer
                            ? plugin.getLanguageManager().getMessage("wab_status_blocked")
                            : plugin.getLanguageManager().getMessage("wab_status_allowed"),
                    detail
            );
            sender.sendMessage(Component.text(statusText).color(blockedForPlayer ? NamedTextColor.RED : NamedTextColor.GREEN));
        }
        return true;
    }

    private String buildDetail(String feature, Instant now, Instant bypassUntil) {
        if (bypassUntil != null) {
            return plugin.getLanguageManager().getMessage("wab_status_bypass_until", formatInstant(bypassUntil));
        }

        if (plugin.getConfigManager().isRecurring(feature)) {
            return plugin.getLanguageManager().getMessage("wab_status_schedule", plugin.getConfigManager().getFormattedSchedule(feature));
        }

        Instant until = plugin.getConfigManager().getRestrictionInstant(feature);
        if (until.equals(Instant.MAX)) {
            return plugin.getLanguageManager().getMessage("wab_status_no_limit");
        }

        Duration left = Duration.between(now, until);
        if (left.isNegative() || left.isZero()) {
            return plugin.getLanguageManager().getMessage("wab_status_unlocked");
        }

        long days = left.toDays();
        long hours = left.toHours() % 24;
        return plugin.getLanguageManager().getMessage("wab_status_time_left", days, hours);
    }

    private List<String> getAllFeatures() {
        List<String> features = new ArrayList<>(List.of("nether", "end", "elytra"));
        features.addAll(plugin.getConfigManager().getCustomWorlds());
        return features;
    }

    private boolean isFeatureValid(String feature) {
        Set<String> all = plugin.getConfigManager().getAllFeatures();
        return all.contains(feature);
    }

    private OfflinePlayer resolvePlayer(String nameOrUuid) {
        Player online = Bukkit.getPlayerExact(nameOrUuid);
        if (online != null) {
            return online;
        }

        try {
            UUID uuid = UUID.fromString(nameOrUuid);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(nameOrUuid);
            return (offline.hasPlayedBefore() || offline.isOnline()) ? offline : null;
        }
    }

    private String formatInstant(Instant instant) {
        ZoneId zone = plugin.getConfigManager().getTimeZone();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone);
        return formatter.format(instant);
    }

    private void sendLocalized(CommandSender sender, NamedTextColor color, String key, Object... args) {
        sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage(key, args)).color(color));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("wab.bypass")) {
            return List.of();
        }

        if (args.length == 1) {
            return Stream.of("bypass", "remove", "status")
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("bypass") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("status"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("bypass") || args[0].equalsIgnoreCase("remove"))) {
            return getAllFeatures().stream()
                    .filter(feature -> feature.startsWith(args[2].toLowerCase()))
                    .toList();
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("bypass")) {
            return Stream.of("3600", "7200", "86400")
                    .filter(duration -> duration.startsWith(args[3]))
                    .toList();
        }

        return List.of();
    }
}
