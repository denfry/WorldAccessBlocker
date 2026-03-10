package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigManagerTest {

    @Test
    void parsesDateInConfiguredTimezone() throws InvalidConfigurationException {
        ConfigManager manager = createManager("""
                language: "en"
                time-zone: "Europe/Moscow"
                nether:
                  disable: true
                  restriction-date: "2025-03-10 00:00:00"
                end:
                  disable: false
                elytra:
                  disable: false
                """);

        assertEquals(Instant.parse("2025-03-09T21:00:00Z"), manager.getNetherRestrictionInstant());
    }

    @Test
    void emptyRecurringPeriodsMeansAlwaysBlocked() throws InvalidConfigurationException {
        ConfigManager manager = createManager("""
                language: "en"
                time-zone: "UTC"
                nether:
                  disable: false
                end:
                  disable: true
                  recurring:
                    periods: []
                elytra:
                  disable: false
                """);

        assertTrue(manager.isRecurring("end"));
        assertTrue(manager.isRestrictionActive("end", Instant.parse("2026-03-10T12:00:00Z")));
    }

    @Test
    void recurringWindowAllowsOnlyConfiguredTime() throws InvalidConfigurationException {
        ConfigManager manager = createManager("""
                language: "en"
                time-zone: "UTC"
                nether:
                  disable: true
                  recurring:
                    periods:
                      - days: [MONDAY]
                        start-time: "10:00"
                        end-time: "12:00"
                end:
                  disable: false
                elytra:
                  disable: false
                """);

        assertFalse(manager.isRestrictionActive("nether", Instant.parse("2026-03-09T11:00:00Z")));
        assertFalse(manager.isRestrictionActive("nether", Instant.parse("2026-03-09T12:00:00Z")));
        assertTrue(manager.isRestrictionActive("nether", Instant.parse("2026-03-09T12:01:00Z")));
    }

    @Test
    void fallbackSpawnWorldsUseFeatureOrDefault() throws InvalidConfigurationException {
        ConfigManager manager = createManager("""
                language: "en"
                time-zone: "UTC"
                nether:
                  disable: true
                end:
                  disable: true
                elytra:
                  disable: true
                fallback-spawns:
                  default: "world"
                  nether: "hub"
                  custom-worlds:
                    lobby_world: "lobby_spawn"
                custom-worlds:
                  lobby_world:
                    disable: true
                """);

        assertEquals("hub", manager.getFallbackSpawnWorld("nether"));
        assertEquals("world", manager.getFallbackSpawnWorld("end"));
        assertEquals("lobby_spawn", manager.getFallbackSpawnWorld("lobby_world"));
    }

    private ConfigManager createManager(String yamlText) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yamlText);

        WorldAccessBlocker plugin = mock(WorldAccessBlocker.class);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ConfigManagerTest"));
        doNothing().when(plugin).saveDefaultConfig();

        ConfigManager manager = new ConfigManager(plugin);
        manager.loadConfigValues();
        return manager;
    }
}
