package net.denfry.worldAccessBlocker.listeners;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.time.Instant;

public class EndBlocker implements Listener {
    private final WorldAccessBlocker plugin;

    public EndBlocker(WorldAccessBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInsertEye(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().isDisableEndPortalActivation()) return;
        Instant now = Instant.now();
        if (!plugin.getConfigManager().isRestrictionActive("end", now)) return;
        if (!plugin.isRestricted(event.getPlayer(), "end")) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            Block block = event.getClickedBlock();
            if (block == null || block.getType() != Material.END_PORTAL_FRAME) return;

            event.setCancelled(true);
            plugin.sendRestrictionMessage(event.getPlayer(), "end");
        }
    }
}