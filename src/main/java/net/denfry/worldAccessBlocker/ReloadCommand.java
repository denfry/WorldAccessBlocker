package net.denfry.worldAccessBlocker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {
    private final WorldAccessBlocker plugin;

    public ReloadCommand(WorldAccessBlocker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!sender.hasPermission("wab.reload")) {
            sender.sendMessage(Component.text("У вас нет прав на использование этой команды!")
                    .color(TextColor.color(0xFF0000)));
            return true;
        }

        plugin.reloadConfig();
        plugin.loadConfigValues();
        sender.sendMessage(Component.text("Конфиг успешно перезагружен!")
                .color(TextColor.color(NamedTextColor.GREEN)));
        return true;
    }
}
