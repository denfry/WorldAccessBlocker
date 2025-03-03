package net.denfry.worldAccessBlocker.listeners;

import net.denfry.worldAccessBlocker.utils.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.plugin.Plugin;

import java.util.Date;

public class PortalBlocker implements Listener {
    private final Plugin plugin;
    private final ConfigManager configManager;

    public PortalBlocker(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!configManager.isDisableNether()) return;
        if (new Date().after(configManager.getNetherRestrictionDate())) return;
        if (event.getTo().getWorld().getEnvironment() != World.Environment.NETHER) return;

        Player player = event.getPlayer();
        event.setCancelled(true);
        player.sendMessage(Component.text("🔥 Вход в Ад запрещён до " + configManager.getNetherRestrictionDate() + "!")
                .color(TextColor.color(0xFF0000)));

        if (Bukkit.getVersion().contains("Folia")) {
            plugin.getLogger().warning("⚠️ Игрок " + player.getName() + " попытался зайти в Ад, но блокировка может не работать на Folia!");
        }
    }
}
