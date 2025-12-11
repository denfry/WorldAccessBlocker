package net.denfry.worldAccessBlocker.listeners;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import net.denfry.worldAccessBlocker.utils.WorldGuardIntegration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class RegionBlockListener implements Listener {
    private final WorldAccessBlocker plugin;
    private final WorldGuardIntegration wg;

    public RegionBlockListener(WorldAccessBlocker plugin, WorldGuardIntegration wg) {
        this.plugin = plugin;
        this.wg = wg;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        handle(event.getPlayer(), event.getTo(), true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;
        handle(event.getPlayer(), event.getTo(), event.isCancelled());
    }

    private void handle(Player player, Location to, boolean cancelIfBlocked) {
        WorldGuardIntegration.BlockDecision decision = wg.check(player, to);
        if (decision == WorldGuardIntegration.BlockDecision.ALLOW) return;
        if (!plugin.isRestricted(player, "region")) return;

        if (cancelIfBlocked && decision == WorldGuardIntegration.BlockDecision.BLOCK) {
            player.teleport(plugin.getOverworldSpawn());
        }
        player.sendMessage(plugin.getLanguageManager().getMessage("region_blocked"));
    }
}

