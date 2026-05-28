package net.denfry.worldAccessBlocker.listeners;

import net.denfry.worldAccessBlocker.utils.LanguageManager;
import net.denfry.worldAccessBlocker.utils.VersionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateNotifier implements Listener {

    private final VersionChecker versionChecker;
    private final LanguageManager languageManager;

    public UpdateNotifier(VersionChecker versionChecker, LanguageManager languageManager) {
        this.versionChecker = versionChecker;
        this.languageManager = languageManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String latest = versionChecker.getLatestVersion();
        if (latest == null || latest.isEmpty()) return;
        if (!event.getPlayer().hasPermission("wab.update-notify")) return;
        String text = languageManager.getMessage("update_available", latest);
        event.getPlayer().sendMessage(Component.text(text).color(NamedTextColor.YELLOW));
    }
}
