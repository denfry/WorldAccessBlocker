package net.denfry.worldAccessBlocker.listeners;

import net.denfry.worldAccessBlocker.utils.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Date;

public class EndBlocker implements Listener {

    private final ConfigManager configManager;

    public EndBlocker(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerInsertEye(PlayerInteractEvent event) {
        if (!configManager.isDisableEnd()) return;
        if (new Date().after(configManager.getEndRestrictionDate())) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            Block block = event.getClickedBlock();
            if (block == null || block.getType() != Material.END_PORTAL_FRAME) return;

            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("🚫 Активация портала в Энд запрещена до " + configManager.getEndRestrictionDate() + "!")
                    .color(TextColor.color(0xFF0000)));
        }
    }
}
