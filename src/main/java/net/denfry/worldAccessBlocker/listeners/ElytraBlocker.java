package net.denfry.worldAccessBlocker.listeners;

import net.denfry.worldAccessBlocker.utils.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Date;

public class ElytraBlocker implements Listener {

    private final ConfigManager configManager;

    public ElytraBlocker(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onElytraUse(PlayerToggleFlightEvent event) {
        if (configManager.isDisableElytra()) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        if (new Date().before(configManager.getElytraRestrictionDate())) {
            event.setCancelled(true);
            player.sendMessage(Component.text("🛑 Полёт на элитрах запрещён до " + configManager.getElytraRestrictionDate() + "!")
                    .color(TextColor.color(0xFF0000)));
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (configManager.isDisableElytra()) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        if (new Date().before(configManager.getElytraRestrictionDate())) {
            ItemStack chestplate = player.getInventory().getChestplate();
            if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                player.getInventory().setChestplate(null);
                player.getInventory().addItem(chestplate);
                player.sendMessage(Component.text("🔻 Элитры сняты!").color(TextColor.color(0xFF0000)));
            }
        }
    }
}
