package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BypassCommand implements CommandExecutor, TabCompleter {
    private final WorldAccessBlocker plugin;
    private final BypassManager bypassManager;
    private final DateTimeFormatter dateFormatter;

    public BypassCommand(WorldAccessBlocker plugin, BypassManager bypassManager) {
        this.plugin = plugin;
        this.bypassManager = bypassManager;
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(plugin.getConfigManager().getTimeZone());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("status")) {
            if (!sender.hasPermission("wab.status")) {
                sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("no_permission")).color(NamedTextColor.RED));
                return true;
            }
            sendStatus(sender);
            return true;
        }

        if (!sender.hasPermission("wab.bypass")) {
            sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("no_permission")).color(NamedTextColor.RED));
            return true;
        }

        switch (sub) {
            case "bypass" -> handleBypass(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "clearexpired", "clear", "cleanup" -> handleClear(sender);
            case "stats" -> handleStats(sender);
            case "gui" -> handleGui(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /wab <bypass|remove|list|clearexpired|status|stats|gui> ...").color(NamedTextColor.YELLOW));
    }

    private void handleBypass(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendUsage(sender);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String feature = args[2].toLowerCase();
        if (!isValidFeature(feature)) {
            sender.sendMessage(Component.text("Invalid feature. Use: nether, end, elytra, or a custom world name.").color(NamedTextColor.RED));
            return;
        }
        if (!sender.hasPermission("wab.bypass." + feature)) {
            sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("no_permission")).color(NamedTextColor.RED));
            return;
        }

        long seconds = parseDurationSeconds(args[3]);
        if (seconds <= 0) {
            sender.sendMessage(Component.text("Invalid duration. Examples: 3600, 30m, 2h, 1d").color(NamedTextColor.RED));
            return;
        }

        String reason = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : "";
        Instant bypassUntil = Instant.now().plusSeconds(seconds);
        bypassManager.grantBypass(target.getUniqueId(), feature, bypassUntil, reason);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        sender.sendMessage(Component.text(
                        plugin.getLanguageManager().getMessage("bypass_granted", feature, name, dateFormatter.format(bypassUntil)))
                .color(NamedTextColor.GREEN));
        plugin.getLogger().info("Bypass granted for " + feature + " to " + name + " until " + bypassUntil + (reason.isBlank() ? "" : " | reason: " + reason));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendUsage(sender);
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String feature = args[2].toLowerCase();
        if (!isValidFeature(feature)) {
            sender.sendMessage(Component.text("Invalid feature. Use: nether, end, elytra, or a custom world name.").color(NamedTextColor.RED));
            return;
        }

        if (bypassManager.removeBypass(target.getUniqueId(), feature)) {
            String name = target.getName() != null ? target.getName() : target.getUniqueId().toString();
            sender.sendMessage(Component.text(
                            plugin.getLanguageManager().getMessage("bypass_removed", feature, name))
                    .color(NamedTextColor.GREEN));
            plugin.getLogger().info("Bypass removed for " + feature + " for " + name);
        } else {
            sender.sendMessage(Component.text(
                    "No active bypass found for " + feature + " for " + (target.getName() == null ? target.getUniqueId() : target.getName())).color(NamedTextColor.YELLOW));
        }
    }

    private void handleList(CommandSender sender) {
        Map<UUID, Map<String, BypassManager.BypassEntry>> data = bypassManager.getBypasses();
        if (data.isEmpty()) {
            sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("bypass_list_empty")).color(NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("bypass_list_header")).color(NamedTextColor.GOLD));
        data.forEach((uuid, features) -> {
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            if (playerName == null) playerName = uuid.toString();
            for (Map.Entry<String, BypassManager.BypassEntry> e : features.entrySet()) {
                String reason = e.getValue().reason == null || e.getValue().reason.isBlank() ? "-" : e.getValue().reason;
                String line = plugin.getLanguageManager().getMessage("bypass_list_item",
                        e.getKey(), playerName, dateFormatter.format(e.getValue().until), reason);
                sender.sendMessage(Component.text(line).color(NamedTextColor.GRAY));
            }
        });
    }

    private void handleClear(CommandSender sender) {
        int removed = bypassManager.clearExpired();
        sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("bypass_cleared", removed)).color(NamedTextColor.GREEN));
    }

    private void handleStats(CommandSender sender) {
        if (!plugin.getConfigManager().isProEdition()) {
            sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("upgrade_hint")).color(NamedTextColor.YELLOW));
            return;
        }
        if (!plugin.getConfigManager().isStatsEnabled()) {
            sender.sendMessage(Component.text("Stats are disabled in config.").color(NamedTextColor.YELLOW));
            return;
        }
        if (!sender.hasPermission("wab.stats")) {
            sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("no_permission")).color(NamedTextColor.RED));
            return;
        }
        var stats = plugin.getStatsTracker().snapshot();
        sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("stats_header")).color(NamedTextColor.GOLD));
        stats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("stats_line", e.getKey(), e.getValue())).color(NamedTextColor.GRAY)));
    }

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.").color(NamedTextColor.RED));
            return;
        }
        if (!plugin.getConfigManager().isProEdition()) {
            sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("upgrade_hint")).color(NamedTextColor.YELLOW));
            return;
        }
        if (!plugin.getConfigManager().isGuiEnabled()) {
            sender.sendMessage(Component.text("GUI is disabled in config.").color(NamedTextColor.YELLOW));
            return;
        }
        if (!player.hasPermission("wab.gui")) {
            player.sendMessage(Component.text(plugin.getLanguageManager().getMessage("no_permission")).color(NamedTextColor.RED));
            return;
        }
        plugin.getGuiManager().open(player);
    }

    private void sendStatus(CommandSender sender) {
        var cfg = plugin.getConfigManager();
        Instant now = Instant.now();
        sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("status_header")).color(NamedTextColor.GOLD));
        sendStatusLine(sender, "nether", "Nether", now);
        sendStatusLine(sender, "end", "End", now);
        sendStatusLine(sender, "elytra", "Elytra", now);
        for (String world : cfg.getCustomWorlds()) {
            sendStatusLine(sender, world, world, now);
        }
        if (!cfg.isProEdition()) {
            sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("upgrade_hint")).color(NamedTextColor.GRAY));
        }
    }

    private void sendStatusLine(CommandSender sender, String feature, String display, Instant now) {
        var cfg = plugin.getConfigManager();
        boolean disabled = cfg.getDisable(feature);
        boolean active = cfg.isRestrictionActive(feature, now);
        String msgKey;
        Object[] args;
        if (!disabled) {
            msgKey = "status_line_allowed";
            args = new Object[]{display};
        } else if (!active) {
            msgKey = "status_line_allowed";
            args = new Object[]{display};
        } else if (cfg.isRecurring(feature)) {
            msgKey = "status_line_recurring";
            args = new Object[]{display, cfg.getFormattedSchedule(feature)};
        } else {
            Instant until = cfg.getRestrictionInstant(feature);
            Duration d = Duration.between(now, until);
            long days = Math.max(0, d.toDays());
            long hours = Math.max(0, d.toHours() % 24);
            msgKey = "status_line_blocked";
            args = new Object[]{display, days, hours};
        }
        sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage(msgKey, args)).color(NamedTextColor.YELLOW));
    }

    private long parseDurationSeconds(String raw) {
        String lower = raw.toLowerCase();
        try {
            if (lower.endsWith("d")) {
                return Long.parseLong(lower.replace("d", "")) * 86400;
            } else if (lower.endsWith("h")) {
                return Long.parseLong(lower.replace("h", "")) * 3600;
            } else if (lower.endsWith("m")) {
                return Long.parseLong(lower.replace("m", "")) * 60;
            } else {
                return Long.parseLong(lower);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean isValidFeature(String feature) {
        return feature.equals("nether") || feature.equals("end") || feature.equals("elytra")
                || feature.equals("region")
                || plugin.getConfigManager().getCustomWorlds().contains(feature);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            Stream<String> base = Stream.of("status", "bypass", "remove", "list", "clearexpired", "stats", "gui");
            completions.addAll(base
                    .filter(cmd -> {
                        if (cmd.equals("status")) return sender.hasPermission("wab.status");
                        if (cmd.equals("stats")) return sender.hasPermission("wab.stats");
                        if (cmd.equals("gui")) return sender.hasPermission("wab.gui");
                        return sender.hasPermission("wab.bypass");
                    })
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("bypass") || args[0].equalsIgnoreCase("remove"))) {
            if (!sender.hasPermission("wab.bypass")) return completions;
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList());
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("bypass") || args[0].equalsIgnoreCase("remove"))) {
            if (!sender.hasPermission("wab.bypass")) return completions;
            List<String> features = Stream.of("nether", "end", "elytra", "region").collect(Collectors.toList());
            features.addAll(plugin.getConfigManager().getCustomWorlds());
            completions.addAll(features.stream()
                    .filter(feature -> feature.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("bypass")) {
            if (!sender.hasPermission("wab.bypass")) return completions;
            completions.addAll(Stream.of("3600", "7200", "86400")
                    .filter(duration -> duration.startsWith(args[3]))
                    .toList());
        }

        return completions;
    }
}
