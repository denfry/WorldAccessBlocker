package net.denfry.worldAccessBlocker.listeners;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.time.Instant;

public class WorldEntryBlocker implements Listener {
    private final WorldAccessBlocker plugin;

    public WorldEntryBlocker(WorldAccessBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) return;
        Player player = event.getPlayer();
        String targetWorldName = event.getTo().getWorld().getName();

        if (isCustomWorldBlockedForPlayer(player, targetWorldName, Instant.now())) {
            event.setCancelled(true);
            plugin.sendRestrictionMessage(player, targetWorldName);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        if (!isCustomWorldBlockedForPlayer(player, worldName, Instant.now())) return;

        Location fallback = plugin.getFallbackSpawn(worldName);
        plugin.getRuntime().runForPlayer(player, () -> {
            plugin.getRuntime().teleportPlayer(player, fallback);
            plugin.sendRestrictionMessage(player, worldName);
        });
    }

    private boolean isCustomWorldBlockedForPlayer(Player player, String worldName, Instant now) {
        if (!plugin.getConfigManager().getCustomWorlds().contains(worldName)) return false;
        if (!plugin.getConfigManager().isCustomWorldDisabled(worldName)) return false;
        if (!plugin.getConfigManager().isRestrictionActive(worldName, now)) return false;
        return plugin.isRestricted(player, worldName);
    }
}
