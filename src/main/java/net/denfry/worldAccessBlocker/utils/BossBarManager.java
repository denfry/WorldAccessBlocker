package net.denfry.worldAccessBlocker.utils;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {
    private final org.bukkit.plugin.Plugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public BossBarManager(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
    }

    public void show(Player player, String feature, Duration duration, Component message) {
        hide(player);
        BossBar bar = BossBar.bossBar(message.colorIfAbsent(NamedTextColor.RED), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        bars.put(player.getUniqueId(), bar);
        player.showBossBar(bar);
        long ticks = Math.max(40, duration.toSeconds() > 0 ? Math.min(duration.toSeconds() * 20, 20 * 60) : 100);
        float decrement = 1f / ticks;
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            BossBar b = bars.get(player.getUniqueId());
            if (b == null) {
                task.cancel();
                return;
            }
            float progress = Math.max(0f, b.progress() - decrement);
            b.progress(progress);
            if (progress <= 0.01f) {
                hide(player);
                task.cancel();
            }
        }, 1L, 1L);
    }

    public void hide(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }
}

