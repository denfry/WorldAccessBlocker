package net.denfry.worldAccessBlocker.runtime;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PaperRuntimeAdapter implements PlatformRuntime {
    private final Plugin plugin;

    public PaperRuntimeAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runRepeatingGlobal(Runnable task, long initialDelayTicks, long periodTicks) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, task, initialDelayTicks, periodTicks);
    }

    @Override
    public void runAsync(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runForPlayer(Player player, Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public void teleportPlayer(Player player, Location location) {
        player.teleport(location);
    }
}
