package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {
    private final WorldAccessBlocker plugin;
    private final ConfigManager configManager;

    public ReloadCommand(WorldAccessBlocker plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("wab.reload")) {
            sender.sendMessage(Component.text("❌ У вас нет прав на использование этой команды!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length > 0 && !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(Component.text("⚠ Использование: /wabreload").color(NamedTextColor.YELLOW));
            return true;
        }

        plugin.reloadConfig();
        configManager.loadConfigValues();

        sender.sendMessage(Component.text("✅ Конфиг успешно перезагружен!").color(NamedTextColor.GREEN));
        plugin.getLogger().info("🔄 Конфигурация WorldAccessBlocker перезагружена командой.");

        return true;
    }
}
