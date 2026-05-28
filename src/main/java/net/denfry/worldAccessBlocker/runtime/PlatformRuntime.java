package net.denfry.worldAccessBlocker.runtime;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface PlatformRuntime {
    void runRepeatingGlobal(Runnable task, long initialDelayTicks, long periodTicks);

    void runAsync(Runnable task);

    void runForPlayer(Player player, Runnable task);

    void teleportPlayer(Player player, Location location);
}
