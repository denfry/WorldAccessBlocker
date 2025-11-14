package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BypassManager {
    private final Map<UUID, Map<String, Instant>> bypasses = new HashMap<>();
    private final WorldAccessBlocker plugin;
    private final File bypassFile;

    public BypassManager(WorldAccessBlocker plugin) {
        this.plugin = plugin;
        this.bypassFile = new File(plugin.getDataFolder(), "bypasses.yml");
    }

    public void grantBypass(UUID playerId, String feature, Instant bypassUntil) {
        bypasses.computeIfAbsent(playerId, k -> new HashMap<>()).put(feature, bypassUntil);
        saveBypasses();
    }

    public boolean removeBypass(UUID playerId, String feature) {
        Map<String, Instant> playerBypasses = bypasses.get(playerId);
        if (playerBypasses != null && playerBypasses.remove(feature) != null) {
            if (playerBypasses.isEmpty()) {
                bypasses.remove(playerId);
            }
            saveBypasses();
            return true;
        }
        return false;
    }

    public boolean isRestricted(UUID playerId, String feature) {
        Map<String, Instant> playerBypasses = bypasses.get(playerId);
        if (playerBypasses != null && playerBypasses.containsKey(feature)) {
            Instant bypassUntil = playerBypasses.get(feature);
            if (Instant.now().isBefore(bypassUntil)) {
                return false;
            } else {
                playerBypasses.remove(feature);
                if (playerBypasses.isEmpty()) {
                    bypasses.remove(playerId);
                }
                saveBypasses();
            }
        }
        return true;
    }

    public void loadBypasses() {
        if (!bypassFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(bypassFile);
        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection section = config.getConfigurationSection(uuidStr);
                if (section == null) continue;
                Map<String, Instant> playerBypasses = new HashMap<>();
                for (String feature : section.getKeys(false)) {
                    long epochSecond = section.getLong(feature);
                    playerBypasses.put(feature, Instant.ofEpochSecond(epochSecond));
                }
                if (!playerBypasses.isEmpty()) {
                    bypasses.put(uuid, playerBypasses);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in bypasses.yml: " + uuidStr);
            }
        }
    }

    public void saveBypasses() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Instant>> entry : bypasses.entrySet()) {
            ConfigurationSection section = config.createSection(entry.getKey().toString());
            for (Map.Entry<String, Instant> f : entry.getValue().entrySet()) {
                section.set(f.getKey(), f.getValue().getEpochSecond());
            }
        }
        try {
            config.save(bypassFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save bypasses: " + e.getMessage());
        }
    }
}