package net.denfry.worldAccessBlocker.listeners;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.PortalCreateEvent;

import java.time.Instant;

public class PortalBlocker implements Listener {
    private final WorldAccessBlocker plugin;

    public PortalBlocker(WorldAccessBlocker plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        Instant now = Instant.now();

        if (event.getCause() == PlayerPortalEvent.TeleportCause.NETHER_PORTAL &&
                plugin.getConfigManager().isDisableNetherTeleportation() &&
                plugin.getConfigManager().isRestrictionActive("nether", now) &&
                plugin.isRestricted(player, "nether")) {
            event.setCancelled(true);
            plugin.sendRestrictionMessage(player, "nether");
        }

        if (event.getCause() == PlayerPortalEvent.TeleportCause.END_PORTAL &&
                plugin.getConfigManager().isDisableEndPortalActivation() &&
                plugin.getConfigManager().isRestrictionActive("end", now) &&
                plugin.isRestricted(player, "end")) {
            event.setCancelled(true);
            plugin.sendRestrictionMessage(player, "end");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPortal(EntityPortalEvent event) {
        if (!plugin.getConfigManager().isDisableNetherTeleportation()) return;
        Instant now = Instant.now();
        if (event.getTo() == null || event.getTo().getWorld().getEnvironment() != World.Environment.NETHER) return;
        if (!plugin.getConfigManager().isRestrictionActive("nether", now)) return;

        if (event.getEntity() instanceof Vehicle vehicle) {
            for (Entity passenger : vehicle.getPassengers()) {
                if (passenger instanceof Player player && plugin.isRestricted(player, "nether")) {
                    event.setCancelled(true);
                    plugin.sendRestrictionMessage(player, "nether");
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortalCreate(PortalCreateEvent event) {
        if (!plugin.getConfigManager().isDisableNetherPortalCreation()) return;
        Instant now = Instant.now();
        if (!plugin.getConfigManager().isRestrictionActive("nether", now)) return;

        Player player = null;
        if (event.getEntity() instanceof Player) {
            player = (Player) event.getEntity();
        } else {
            for (Entity entity : event.getWorld().getNearbyEntities(event.getBlocks().getFirst().getLocation(), 5, 5, 5)) {
                if (entity instanceof Player) {
                    player = (Player) entity;
                    break;
                }
            }
        }

        if (player != null && plugin.isRestricted(player, "nether") &&
                player.getGameMode() != org.bukkit.GameMode.CREATIVE &&
                player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            event.setCancelled(true);
            plugin.sendRestrictionMessage(player, "nether");
        } else if (player == null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (!plugin.getConfigManager().isDisableNetherPortalCreation()) return;
        Instant now = Instant.now();
        if (!plugin.getConfigManager().isRestrictionActive("nether", now)) return;

        if (event.getItem().getType() == Material.FIRE_CHARGE || event.getItem().getType() == Material.FLINT_AND_STEEL) {
            Location targetLoc = event.getBlock().getLocation().add(event.getVelocity().normalize());
            if (targetLoc.getBlock().getType() == Material.OBSIDIAN) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl)) return;
        if (!(pearl.getShooter() instanceof Player player)) return;
        if (event.getTo() == null) return;

        Instant now = Instant.now();
        World.Environment targetEnv = event.getTo().getWorld().getEnvironment();

        if (targetEnv == World.Environment.NETHER &&
                plugin.getConfigManager().isDisableNetherTeleportation() &&
                plugin.getConfigManager().isRestrictionActive("nether", now) &&
                plugin.isRestricted(player, "nether")) {
            event.setCancelled(true);
            plugin.sendRestrictionMessage(player, "nether");
            return;
        }

        if (targetEnv == World.Environment.THE_END &&
                plugin.getConfigManager().isDisableEndPortalActivation() &&
                plugin.getConfigManager().isRestrictionActive("end", now) &&
                plugin.isRestricted(player, "end")) {
            event.setCancelled(true);
            plugin.sendRestrictionMessage(player, "end");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World.Environment env = player.getWorld().getEnvironment();
        Instant now = Instant.now();

        if (env == World.Environment.NETHER &&
                plugin.getConfigManager().isDisableNether() &&
                plugin.getConfigManager().isRestrictionActive("nether", now) &&
                plugin.isRestricted(player, "nether")) {
            player.teleport(plugin.getOverworldSpawn());
            plugin.sendRestrictionMessage(player, "nether");
        }

        if (env == World.Environment.THE_END &&
                plugin.getConfigManager().isDisableEnd() &&
                plugin.getConfigManager().isRestrictionActive("end", now) &&
                plugin.isRestricted(player, "end")) {
            player.teleport(plugin.getOverworldSpawn());
            plugin.sendRestrictionMessage(player, "end");
        }
    }
}