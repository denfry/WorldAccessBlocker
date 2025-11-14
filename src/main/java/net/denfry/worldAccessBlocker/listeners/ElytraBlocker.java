package net.denfry.worldAccessBlocker.listeners;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;

public class ElytraBlocker implements Listener {
    private final WorldAccessBlocker plugin;

    public ElytraBlocker(WorldAccessBlocker plugin) {
        this.plugin = plugin;
        startElytraCheckTask();
    }

    private void startElytraCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfigManager().isDisableElytraEquip()) return;
                Instant now = Instant.now();
                if (!plugin.getConfigManager().isRestrictionActive("elytra_equip", now)) return;

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
                        continue;
                    if (!plugin.isRestricted(player, "elytra")) continue;

                    PlayerInventory inventory = player.getInventory();
                    ItemStack chestplate = inventory.getChestplate();
                    if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                        int firstEmptySlot = inventory.firstEmpty();
                        if (firstEmptySlot != -1) {
                            inventory.setItem(firstEmptySlot, chestplate.clone());
                            inventory.setChestplate(null);
                            plugin.sendRestrictionMessage(player, "elytra_equip");
                        } else {
                            player.getWorld().dropItemNaturally(player.getLocation(), chestplate.clone());
                            inventory.setChestplate(null);
                            plugin.sendRestrictionMessage(player, "elytra_equip");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.isGliding()) return;
        if (!plugin.getConfigManager().isDisableElytraFlight()) return;
        Instant now = Instant.now();
        if (!plugin.getConfigManager().isRestrictionActive("elytra", now)) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (!plugin.isRestricted(player, "elytra")) return;

        event.setCancelled(true);
        plugin.sendRestrictionMessage(player, "elytra");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getConfigManager().isDisableElytraEquip()) return;
        Instant now = Instant.now();
        if (!plugin.getConfigManager().isRestrictionActive("elytra_equip", now)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (!plugin.isRestricted(player, "elytra")) return;

        ItemStack cursor = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();

        if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getRawSlot() == 38) {
            if (cursor.getType() == Material.ELYTRA || currentItem != null && currentItem.getType() == Material.ELYTRA) {
                event.setCancelled(true);
                plugin.sendRestrictionMessage(player, "elytra_equip");
            }
        } else if (event.isShiftClick() && currentItem != null && currentItem.getType() == Material.ELYTRA) {
            if (player.getInventory().getChestplate() == null &&
                    event.getInventory().getType() != InventoryType.CRAFTING) {
                event.setCancelled(true);
                plugin.sendRestrictionMessage(player, "elytra_equip");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!plugin.getConfigManager().isDisableElytraEquip()) return;
        Instant now = Instant.now();
        if (!plugin.getConfigManager().isRestrictionActive("elytra_equip", now)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (!plugin.isRestricted(player, "elytra")) return;

        if (event.getRawSlots().contains(38) && event.getNewItems().values().stream()
                .anyMatch(item -> item.getType() == Material.ELYTRA)) {
            event.setCancelled(true);
            plugin.sendRestrictionMessage(player, "elytra_equip");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().isDisableElytraEquip()) return;
        Instant now = Instant.now();
        if (!plugin.getConfigManager().isRestrictionActive("elytra_equip", now)) return;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (!plugin.isRestricted(player, "elytra")) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.ELYTRA && event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND &&
                    player.getInventory().getChestplate() == null) {
                event.setCancelled(true);
                plugin.sendRestrictionMessage(player, "elytra_equip");
            }
        }
    }
}