package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class BypassManager {
    public static class BypassEntry {
        public Instant until;
        public String reason;
    }

    private final Map<UUID, Map<String, BypassEntry>> bypasses = new HashMap<>();
    private final WorldAccessBlocker plugin;
    private final File bypassFile;

    public BypassManager(WorldAccessBlocker plugin) {
        this.plugin = plugin;
        this.bypassFile = new File(plugin.getDataFolder(), "bypasses.yml");
    }

    public void grantBypass(UUID playerId, String feature, Instant bypassUntil, String reason) {
        grantBypass(playerId, feature, bypassUntil, reason, true);
    }

    public void grantBypass(UUID playerId, String feature, Instant bypassUntil, String reason, boolean broadcast) {
        BypassEntry entry = new BypassEntry();
        entry.until = bypassUntil;
        entry.reason = reason;
        bypasses.computeIfAbsent(playerId, k -> new HashMap<>()).put(feature, entry);
        saveBypasses();
        if (broadcast && plugin.getSyncManager() != null) {
            plugin.getSyncManager().broadcastGrant(playerId, feature, bypassUntil, reason);
        }
    }

    public boolean removeBypass(UUID playerId, String feature) {
        Map<String, BypassEntry> playerBypasses = bypasses.get(playerId);
        if (playerBypasses != null && playerBypasses.remove(feature) != null) {
            if (playerBypasses.isEmpty()) {
                bypasses.remove(playerId);
            }
            saveBypasses();
            if (plugin.getSyncManager() != null) {
                plugin.getSyncManager().broadcastRemove(playerId, feature);
            }
            return true;
        }
        return false;
    }

    public boolean isRestricted(UUID playerId, String feature) {
        Map<String, BypassEntry> playerBypasses = bypasses.get(playerId);
        if (playerBypasses != null && playerBypasses.containsKey(feature)) {
            Instant bypassUntil = playerBypasses.get(feature).until;
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
                Map<String, BypassEntry> playerBypasses = new HashMap<>();
                for (String feature : section.getKeys(false)) {
                    if (section.isLong(feature)) {
                        // legacy format
                        BypassEntry entry = new BypassEntry();
                        entry.until = Instant.ofEpochSecond(section.getLong(feature));
                        playerBypasses.put(feature, entry);
                    } else {
                        ConfigurationSection featureSec = section.getConfigurationSection(feature);
                        if (featureSec == null) continue;
                        long epochSecond = featureSec.getLong("until", 0L);
                        if (epochSecond <= 0) continue;
                        BypassEntry entry = new BypassEntry();
                        entry.until = Instant.ofEpochSecond(epochSecond);
                        entry.reason = featureSec.getString("reason", "");
                        playerBypasses.put(feature, entry);
                    }
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
        for (Map.Entry<UUID, Map<String, BypassEntry>> entry : bypasses.entrySet()) {
            ConfigurationSection section = config.createSection(entry.getKey().toString());
            for (Map.Entry<String, BypassEntry> f : entry.getValue().entrySet()) {
                ConfigurationSection featureSec = section.createSection(f.getKey());
                featureSec.set("until", f.getValue().until.getEpochSecond());
                featureSec.set("reason", f.getValue().reason == null ? "" : f.getValue().reason);
            }
        }
        try {
            config.save(bypassFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save bypasses: " + e.getMessage());
        }
    }

    public int clearExpired() {
        int removed = 0;
        Instant now = Instant.now();
        for (UUID uuid : new ArrayList<>(bypasses.keySet())) {
            Map<String, BypassEntry> entries = bypasses.get(uuid);
            if (entries == null) continue;
            for (String feature : new ArrayList<>(entries.keySet())) {
                if (now.isAfter(entries.get(feature).until)) {
                    entries.remove(feature);
                    removed++;
                }
            }
            if (entries.isEmpty()) {
                bypasses.remove(uuid);
            }
        }
        if (removed > 0) saveBypasses();
        return removed;
    }

    public Map<UUID, Map<String, BypassEntry>> getBypasses() {
        return Collections.unmodifiableMap(bypasses);
    }
}
