package net.denfry.worldAccessBlocker.runtime;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class PlatformRuntimeFactory {
    private PlatformRuntimeFactory() {
    }

    public static PlatformRuntime create(Plugin plugin) {
        if (isFoliaRuntime()) {
            return new FoliaRuntimeAdapter(plugin);
        }
        return new PaperRuntimeAdapter(plugin);
    }

    public static boolean isFoliaRuntime() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return Bukkit.getName().toLowerCase().contains("folia") || Bukkit.getVersion().contains("Folia");
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
