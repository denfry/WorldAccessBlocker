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
    private final LanguageManager languageManager;

    public ReloadCommand(WorldAccessBlocker plugin, ConfigManager configManager, LanguageManager languageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("wab.reload")) {
            sender.sendMessage(Component.text(languageManager.getMessage("no_permission")).color(NamedTextColor.RED));
            return true;
        }

        if (args.length > 0 && !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(Component.text(languageManager.getMessage("invalid_usage")).color(NamedTextColor.YELLOW));
            return true;
        }

        plugin.reloadConfig();
        configManager.loadConfigValues();
        languageManager.reloadLanguage();

        sender.sendMessage(Component.text(languageManager.getMessage("config_reloaded")).color(NamedTextColor.GREEN));
        plugin.getLogger().info(languageManager.getMessage("log_config_reloaded"));
        return true;
    }
}