package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Instant;

public class RestrictionEnforcer implements Runnable {
    private final WorldAccessBlocker plugin;

    public RestrictionEnforcer(WorldAccessBlocker plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Instant now = Instant.now();
        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            String worldName = world.getName();

            if (world.getEnvironment() == World.Environment.NETHER &&
                    plugin.getConfigManager().isDisableNether() &&
                    plugin.getConfigManager().isRestrictionActive("nether", now) &&
                    plugin.isRestricted(player, "nether")) {
                player.teleport(plugin.getOverworldSpawn());
                plugin.sendRestrictionMessage(player, "nether");
            } else if (world.getEnvironment() == World.Environment.THE_END &&
                    plugin.getConfigManager().isDisableEnd() &&
                    plugin.getConfigManager().isRestrictionActive("end", now) &&
                    plugin.isRestricted(player, "end")) {
                player.teleport(plugin.getOverworldSpawn());
                plugin.sendRestrictionMessage(player, "end");
            } else if (plugin.getConfigManager().getCustomWorlds().contains(worldName) &&
                    plugin.getConfigManager().isCustomWorldDisabled(worldName) &&
                    plugin.getConfigManager().isRestrictionActive(worldName, now) &&
                    plugin.isRestricted(player, worldName)) {
                player.teleport(plugin.getOverworldSpawn());
                plugin.sendRestrictionMessage(player, worldName);
            }
        }
    }
}