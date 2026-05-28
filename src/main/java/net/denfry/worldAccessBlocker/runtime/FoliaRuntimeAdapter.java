package net.denfry.worldAccessBlocker.runtime;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

public class FoliaRuntimeAdapter implements PlatformRuntime {
    private final Plugin plugin;
    private final PlatformRuntime fallback;

    public FoliaRuntimeAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.fallback = new PaperRuntimeAdapter(plugin);
    }

    @Override
    public void runRepeatingGlobal(Runnable task, long initialDelayTicks, long periodTicks) {
        try {
            Object globalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Method runAtFixedRate = globalRegionScheduler.getClass().getMethod(
                    "runAtFixedRate",
                    Plugin.class,
                    Consumer.class,
                    long.class,
                    long.class
            );
            Consumer<Object> consumer = ignored -> task.run();
            runAtFixedRate.invoke(globalRegionScheduler, plugin, consumer, initialDelayTicks, periodTicks);
        } catch (Exception ex) {
            fallback.runRepeatingGlobal(task, initialDelayTicks, periodTicks);
        }
    }

    @Override
    public void runAsync(Runnable task) {
        try {
            Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            Method runNow = asyncScheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
            Consumer<Object> consumer = ignored -> task.run();
            runNow.invoke(asyncScheduler, plugin, consumer);
        } catch (Exception ex) {
            fallback.runAsync(task);
        }
    }

    @Override
    public void runForPlayer(Player player, Runnable task) {
        try {
            Object entityScheduler = player.getClass().getMethod("getScheduler").invoke(player);
            Method run = Arrays.stream(entityScheduler.getClass().getMethods())
                    .filter(m -> m.getName().equals("run"))
                    .filter(m -> m.getParameterCount() >= 3)
                    .findFirst()
                    .orElseThrow();
            Consumer<Object> consumer = ignored -> task.run();
            Object[] args = new Object[run.getParameterCount()];
            Class<?>[] paramTypes = run.getParameterTypes();
            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> type = paramTypes[i];
                if (Plugin.class.isAssignableFrom(type)) {
                    args[i] = plugin;
                } else if (Consumer.class.isAssignableFrom(type)) {
                    args[i] = consumer;
                } else if (Runnable.class.isAssignableFrom(type)) {
                    args[i] = null;
                } else if (type == long.class || type == Long.class) {
                    args[i] = 0L;
                } else {
                    args[i] = null;
                }
            }
            run.invoke(entityScheduler, args);
        } catch (Exception ex) {
            fallback.runForPlayer(player, task);
        }
    }

    @Override
    public void teleportPlayer(Player player, Location location) {
        player.teleport(location);
    }
}
