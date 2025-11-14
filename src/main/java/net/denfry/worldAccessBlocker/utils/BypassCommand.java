package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BypassCommand implements CommandExecutor, TabCompleter {
    private final WorldAccessBlocker plugin;
    private final BypassManager bypassManager;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public BypassCommand(WorldAccessBlocker plugin, BypassManager bypassManager) {
        this.plugin = plugin;
        this.bypassManager = bypassManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("wab.bypass")) {
            sender.sendMessage(Component.text(plugin.getLanguageManager().getMessage("no_permission")).color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /wab <bypass|remove> <player> <feature> [duration]").color(NamedTextColor.YELLOW));
            return true;
        }

        if (args[0].equalsIgnoreCase("bypass")) {
            if (args.length < 4) {
                sender.sendMessage(Component.text("Usage: /wab bypass <player> <feature> <duration>").color(NamedTextColor.YELLOW));
                return true;
            }

            Player player = plugin.getServer().getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED));
                return true;
            }

            String feature = args[2].toLowerCase();
            if (!feature.equals("nether") && !feature.equals("end") && !feature.equals("elytra") &&
                    !plugin.getConfigManager().getCustomWorlds().contains(feature)) {
                sender.sendMessage(Component.text("Invalid feature. Use: nether, end, elytra, or a custom world name.").color(NamedTextColor.RED));
                return true;
            }

            long seconds;
            try {
                seconds = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid duration. Use a number (seconds).").color(NamedTextColor.RED));
                return true;
            }

            Instant bypassUntil = Instant.now().plusSeconds(seconds);
            bypassManager.grantBypass(player.getUniqueId(), feature, bypassUntil);
            sender.sendMessage(Component.text(
                            plugin.getLanguageManager().getMessage("bypass_granted", feature, player.getName(), DATE_FORMATTER.format(bypassUntil)))
                    .color(NamedTextColor.GREEN));
            plugin.getLogger().info("Bypass granted for " + feature + " to " + player.getName() + " until " + bypassUntil);
            return true;
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /wab remove <player> <feature>").color(NamedTextColor.YELLOW));
                return true;
            }

            Player player = plugin.getServer().getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(Component.text("Player not found.").color(NamedTextColor.RED));
                return true;
            }

            String feature = args[2].toLowerCase();
            if (!feature.equals("nether") && !feature.equals("end") && !feature.equals("elytra") &&
                    !plugin.getConfigManager().getCustomWorlds().contains(feature)) {
                sender.sendMessage(Component.text("Invalid feature. Use: nether, end, elytra, or a custom world name.").color(NamedTextColor.RED));
                return true;
            }

            if (bypassManager.removeBypass(player.getUniqueId(), feature)) {
                sender.sendMessage(Component.text(
                                plugin.getLanguageManager().getMessage("bypass_removed", feature, player.getName()))
                        .color(NamedTextColor.GREEN));
                plugin.getLogger().info("Bypass removed for " + feature + " for " + player.getName());
            } else {
                sender.sendMessage(Component.text(
                        "No active bypass found for " + feature + " for " + player.getName()).color(NamedTextColor.YELLOW));
            }
            return true;
        }

        sender.sendMessage(Component.text("Usage: /wab <bypass|remove> <player> <feature> [duration]").color(NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("wab.bypass")) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(Stream.of("bypass", "remove")
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("bypass") || args[0].equalsIgnoreCase("remove"))) {
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList());
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("bypass") || args[0].equalsIgnoreCase("remove"))) {
            List<String> features = Stream.of("nether", "end", "elytra").collect(Collectors.toList());
            features.addAll(plugin.getConfigManager().getCustomWorlds());
            completions.addAll(features.stream()
                    .filter(feature -> feature.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("bypass")) {
            completions.addAll(Stream.of("3600", "7200", "86400")
                    .filter(duration -> duration.startsWith(args[3]))
                    .toList());
        }

        return completions;
    }
}