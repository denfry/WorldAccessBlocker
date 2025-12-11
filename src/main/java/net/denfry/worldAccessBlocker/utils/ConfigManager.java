package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ConfigManager {
    private final WorldAccessBlocker plugin;
    private boolean disableNether, disableEnd, disableElytra;
    private boolean disableNetherPortalCreation, disableNetherTeleportation;
    private boolean disableEndPortalActivation;
    private boolean disableElytraEquip, disableElytraFlight;
    private Instant netherRestrictionInstant, endRestrictionInstant, elytraRestrictionInstant;
    private ZoneId timeZone;
    private String lang;
    private boolean adminAlert;
    private String notificationMode;
    private String notificationSound;
    private boolean notificationBossbar;
    private boolean useCustomSpawn;
    private String customSpawnWorld;
    private double customSpawnX, customSpawnY, customSpawnZ, customSpawnYaw, customSpawnPitch;
    private boolean updateCheckEnabled;
    private String modrinthId;
    private boolean autoCleanBypasses;
    private boolean proEdition;
    private boolean discordEnabled;
    private String discordWebhook;
    private boolean discordSendBlock;
    private boolean discordSendUpdate;
    private boolean guiEnabled;
    private boolean statsEnabled;
    private boolean syncEnabled;
    private String syncChannel;

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_DATE = "2099-12-31 23:59:59";

    private final Map<String, Boolean> customWorldDisables = new HashMap<>();
    private final Map<String, Instant> customWorldRestrictionInstants = new HashMap<>();
    private final Map<String, List<SchedulePeriod>> recurringPeriods = new HashMap<>();
    private final Map<String, Boolean> useRecurring = new HashMap<>();
    private final Map<String, Set<LocalDate>> skipDates = new HashMap<>();
    private final Map<String, List<OneTimeWindow>> oneTimeWindows = new HashMap<>();

    private static class SchedulePeriod {
        List<DayOfWeek> days = new ArrayList<>();
        LocalTime start;
        LocalTime end;
    }

    private static class OneTimeWindow {
        Instant start;
        Instant end;
    }

    public ConfigManager(WorldAccessBlocker plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public void loadConfigValues() {
        FileConfiguration config = plugin.getConfig();

        recurringPeriods.clear();
        useRecurring.clear();
        skipDates.clear();
        oneTimeWindows.clear();

        proEdition = "pro".equalsIgnoreCase(config.getString("edition", "free"));

        lang = config.getString("language", "en");
        notificationMode = config.getString("notifications.mode", "chat").toLowerCase(Locale.ROOT);
        adminAlert = proEdition && config.getBoolean("notifications.admin-alert", true);
        notificationSound = config.getString("notifications.sound", "ENTITY_VILLAGER_NO");
        notificationBossbar = proEdition && config.getBoolean("notifications.bossbar", true);

        discordEnabled = proEdition && config.getBoolean("discord.enabled", false);
        discordWebhook = config.getString("discord.webhook-url", "");
        discordSendBlock = config.getBoolean("discord.send-on-block", true);
        discordSendUpdate = config.getBoolean("discord.send-on-update", true);

        guiEnabled = proEdition && config.getBoolean("gui.enabled", true);
        statsEnabled = proEdition && config.getBoolean("stats.enabled", true);
        syncEnabled = proEdition && config.getBoolean("sync.enabled", false);
        syncChannel = config.getString("sync.channel", "wab:sync");

        useCustomSpawn = proEdition && config.getBoolean("teleport.use-custom-spawn", false);
        customSpawnWorld = config.getString("teleport.world", "world");
        customSpawnX = config.getDouble("teleport.x", 0.5);
        customSpawnY = config.getDouble("teleport.y", 64);
        customSpawnZ = config.getDouble("teleport.z", 0.5);
        customSpawnYaw = config.getDouble("teleport.yaw", 0.0);
        customSpawnPitch = config.getDouble("teleport.pitch", 0.0);

        updateCheckEnabled = config.getBoolean("update-check.enabled", true);
        modrinthId = config.getString("update-check.modrinth-id", "worldaccessblocker");
        autoCleanBypasses = config.getBoolean("bypass.auto-clean-expired", true);

        String timeZoneId = config.getString("time-zone", "UTC");
        try {
            timeZone = ZoneId.of(timeZoneId);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid time zone: " + timeZoneId + ". Falling back to UTC.");
            timeZone = ZoneId.of("UTC");
        }

        disableNether = config.getBoolean("nether.disable", true);
        disableEnd = config.getBoolean("end.disable", true);
        disableElytra = config.getBoolean("elytra.disable", true);

        loadRecurring("nether", config.getConfigurationSection("nether.recurring"));
        loadRecurring("end", config.getConfigurationSection("end.recurring"));
        loadRecurring("elytra", config.getConfigurationSection("elytra.recurring"));

        if (proEdition) {
            loadSkipDates("nether", config.getConfigurationSection("nether"));
            loadSkipDates("end", config.getConfigurationSection("end"));
            loadSkipDates("elytra", config.getConfigurationSection("elytra"));

            loadOneTimeWindows("nether", config.getConfigurationSection("nether"));
            loadOneTimeWindows("end", config.getConfigurationSection("end"));
            loadOneTimeWindows("elytra", config.getConfigurationSection("elytra"));
        }

        convertOldDaysFormat("nether", config);
        convertOldDaysFormat("end", config);
        convertOldDaysFormat("elytra", config);

        disableNetherPortalCreation = config.getBoolean("nether.disable-portal-creation", true);
        disableNetherTeleportation = config.getBoolean("nether.disable-teleportation", true);
        disableEndPortalActivation = config.getBoolean("end.disable-portal-activation", true);
        disableElytraEquip = config.getBoolean("elytra.disable-equip", true);
        disableElytraFlight = config.getBoolean("elytra.disable-flight", true);

        netherRestrictionInstant = parseDate(config.getString("nether.restriction-date", DEFAULT_DATE), "Nether");
        endRestrictionInstant = parseDate(config.getString("end.restriction-date", DEFAULT_DATE), "End");
        elytraRestrictionInstant = parseDate(config.getString("elytra.restriction-date", DEFAULT_DATE), "Elytra");

        customWorldDisables.clear();
        customWorldRestrictionInstants.clear();
        ConfigurationSection customSection = config.getConfigurationSection("custom-worlds");
        if (customSection != null) {
            for (String worldName : customSection.getKeys(false)) {
                ConfigurationSection worldSec = customSection.getConfigurationSection(worldName);
                if (worldSec == null) continue;

                boolean disable = worldSec.getBoolean("disable", true);
                customWorldDisables.put(worldName, disable);

                String dateStr = worldSec.getString("restriction-date", DEFAULT_DATE);
                customWorldRestrictionInstants.put(worldName, parseDate(dateStr, "Custom world " + worldName));

                loadRecurring(worldName, worldSec.getConfigurationSection("recurring"));
                convertOldDaysFormat(worldName, worldSec);
                if (proEdition) {
                    loadSkipDates(worldName, worldSec);
                    loadOneTimeWindows(worldName, worldSec);
                }
            }
        }
    }

    private void loadRecurring(String feature, ConfigurationSection section) {
        if (section == null || !section.contains("periods")) return;

        List<Map<?, ?>> rawMaps = section.getMapList("periods");
        List<SchedulePeriod> periods = new ArrayList<>();

        for (Map<?, ?> map : rawMaps) {
            Object daysObj = map.get("days");
            if (daysObj == null) continue;

            if (!(daysObj instanceof List)) {
                plugin.getLogger().warning("Invalid 'days' format for " + feature);
                continue;
            }

            @SuppressWarnings("unchecked")
            List<String> daysStr = (List<String>) daysObj;
            if (daysStr.isEmpty()) continue;

            SchedulePeriod p = new SchedulePeriod();
            for (String d : daysStr) {
                try {
                    p.days.add(DayOfWeek.valueOf(d.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid day of week: " + d + " for " + feature);
                }
            }
            if (p.days.isEmpty()) continue;

            Object startObj = map.get("start-time");
            Object endObj = map.get("end-time");

            if (startObj instanceof String && endObj instanceof String) {
                p.start = parseTime((String) startObj, feature);
                p.end = parseTime((String) endObj, feature);

                if (p.start == null || p.end == null) {
                    plugin.getLogger().warning("Invalid time format in " + feature + ": " + startObj + " - " + endObj);
                    continue;
                }

                if (p.end.isBefore(p.start)) {
                    plugin.getLogger().warning("End time before start time for " + feature + " - skipping period");
                    continue;
                }
            } else if (startObj != null || endObj != null) {
                plugin.getLogger().warning("Both start-time and end-time must be strings for " + feature + " - skipping period");
                continue;
            }

            periods.add(p);
        }

        if (!periods.isEmpty()) {
            recurringPeriods.put(feature, periods);
            useRecurring.put(feature, true);
        }
    }

    private void convertOldDaysFormat(String feature, ConfigurationSection parent) {
        if (!parent.contains("recurring-allowed-days") || parent.contains("recurring.periods")) return;

        List<String> oldDays = parent.getStringList("recurring-allowed-days");
        if (oldDays.isEmpty()) return;

        SchedulePeriod p = new SchedulePeriod();
        for (String d : oldDays) {
            try {
                p.days.add(DayOfWeek.valueOf(d.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (p.days.isEmpty()) return;

        recurringPeriods.computeIfAbsent(feature, k -> new ArrayList<>()).add(p);
        useRecurring.put(feature, true);
        plugin.getLogger().info("Converted old recurring-allowed-days → new format for " + feature);
    }

    private void loadSkipDates(String feature, ConfigurationSection section) {
        if (section == null) return;
        List<String> rawDates = section.getStringList("skip-dates");
        if (rawDates == null || rawDates.isEmpty()) return;

        Set<LocalDate> dates = new HashSet<>();
        for (String raw : rawDates) {
            try {
                dates.add(LocalDate.parse(raw.trim()));
            } catch (DateTimeParseException e) {
                plugin.getLogger().warning("Invalid skip-date for " + feature + ": " + raw);
            }
        }
        if (!dates.isEmpty()) {
            skipDates.put(feature, dates);
        }
    }

    private void loadOneTimeWindows(String feature, ConfigurationSection section) {
        if (section == null || !section.contains("one-time-windows")) return;
        List<Map<?, ?>> windows = section.getMapList("one-time-windows");
        if (windows.isEmpty()) return;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        List<OneTimeWindow> parsed = new ArrayList<>();
        for (Map<?, ?> raw : windows) {
            Object startObj = raw.get("start");
            Object endObj = raw.get("end");
            if (!(startObj instanceof String) || !(endObj instanceof String)) continue;
            try {
                Instant start = LocalDateTime.parse(((String) startObj).trim(), formatter).atZone(timeZone).toInstant();
                Instant end = LocalDateTime.parse(((String) endObj).trim(), formatter).atZone(timeZone).toInstant();
                if (end.isBefore(start)) {
                    plugin.getLogger().warning("one-time-windows: end before start for " + feature);
                    continue;
                }
                OneTimeWindow window = new OneTimeWindow();
                window.start = start;
                window.end = end;
                parsed.add(window);
            } catch (DateTimeParseException e) {
                plugin.getLogger().warning("Invalid one-time window for " + feature + ": " + startObj + " - " + endObj);
            }
        }
        if (!parsed.isEmpty()) {
            oneTimeWindows.put(feature, parsed);
        }
    }

    private LocalTime parseTime(String raw, String feature) {
        String cleaned = raw.trim()
                .replace("p.m.", "PM")
                .replace("a.m.", "AM")
                .replace("p.m", "PM")
                .replace("a.m", "AM")
                .replace("p.m", "PM")
                .replace("a.m", "AM")
                .replace(" pm", " PM")
                .replace(" am", " AM")
                .toUpperCase(Locale.ROOT);

        List<String> patterns = List.of("H:mm", "HH:mm", "H:mm:ss", "h:mm a", "h a", "h:mm a");
        for (String pattern : patterns) {
            try {
                return LocalTime.parse(cleaned, DateTimeFormatter.ofPattern(pattern).withLocale(Locale.US));
            } catch (DateTimeParseException ignored) {
            }
        }
        plugin.getLogger().warning("Invalid time format for " + feature + ": " + raw);
        return null;
    }

    public boolean isRestrictionActive(String feature, Instant now) {
        if (!getDisable(feature)) return false;

        if (proEdition && isInsideOneTimeWindow(feature, now)) {
            return false;
        }

        if (proEdition && isSkipDate(feature, now)) {
            return true;
        }

        Boolean recurring = useRecurring.get(feature);
        if (recurring != null && recurring) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(now, timeZone);
            DayOfWeek day = zdt.getDayOfWeek();
            LocalTime time = zdt.toLocalTime();

            List<SchedulePeriod> periods = recurringPeriods.getOrDefault(feature, Collections.emptyList());
            return periods.stream().noneMatch(p ->
                    p.days.contains(day) &&
                            (p.start == null ||
                                    (time.isAfter(p.start) || time.equals(p.start)) &&
                                            (time.isBefore(p.end) || time.equals(p.end))));
        }

        Instant until = getRestrictionInstant(feature);
        return now.isBefore(until);
    }

    private boolean isSkipDate(String feature, Instant now) {
        Set<LocalDate> dates = skipDates.get(feature);
        if (dates == null || dates.isEmpty()) return false;
        LocalDate today = LocalDateTime.ofInstant(now, timeZone).toLocalDate();
        return dates.contains(today);
    }

    private boolean isInsideOneTimeWindow(String feature, Instant now) {
        List<OneTimeWindow> windows = oneTimeWindows.get(feature);
        if (windows == null || windows.isEmpty()) return false;
        for (OneTimeWindow window : windows) {
            if ((now.equals(window.start) || now.isAfter(window.start)) && now.isBefore(window.end)) {
                return true;
            }
        }
        return false;
    }

    public boolean getDisable(String feature) {
        return switch (feature) {
            case "nether" -> disableNether;
            case "end" -> disableEnd;
            case "elytra", "elytra_equip" -> disableElytra;
            default -> customWorldDisables.getOrDefault(feature, false);
        };
    }

    public Instant getRestrictionInstant(String feature) {
        return switch (feature) {
            case "nether" -> netherRestrictionInstant;
            case "end" -> endRestrictionInstant;
            case "elytra", "elytra_equip" -> elytraRestrictionInstant;
            default -> customWorldRestrictionInstants.getOrDefault(feature, Instant.MAX);
        };
    }

    public boolean isRecurring(String feature) {
        return Boolean.TRUE.equals(useRecurring.get(feature));
    }

    public String getFormattedSchedule(String feature) {
        List<SchedulePeriod> periods = recurringPeriods.getOrDefault(feature, Collections.emptyList());
        if (periods.isEmpty()) {
            return "ru".equals(lang) ? "никогда" : "never";
        }

        Map<String, List<DayOfWeek>> timeGroups = new LinkedHashMap<>();
        List<DayOfWeek> allDay = new ArrayList<>();

        for (SchedulePeriod p : periods) {
            String key = p.start == null ? "ALL_DAY" : p.start + "-" + p.end;
            if ("ALL_DAY".equals(key)) {
                allDay.addAll(p.days);
            } else {
                timeGroups.computeIfAbsent(key, k -> new ArrayList<>()).addAll(p.days);
            }
        }

        List<String> parts = new ArrayList<>();

        if (!allDay.isEmpty()) {
            String daysStr = formatDays(allDay);
            parts.add("ru".equals(lang) ? "по " + daysStr + " весь день" : "all day on " + daysStr);
        }

        for (var e : timeGroups.entrySet()) {
            String[] times = e.getKey().split("-");
            String daysStr = formatDays(e.getValue());
            parts.add(("ru".equals(lang) ? "по " : "on ") + daysStr +
                    " с " + times[0] + " до " + times[1]);
        }

        return String.join("ru".equals(lang) ? ", " : ", ", parts) + ".";
    }

    private String formatDays(List<DayOfWeek> days) {
        if (days.isEmpty()) return "";

        List<DayOfWeek> sorted = days.stream()
                .distinct()
                .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                .toList();

        Map<DayOfWeek, String> map = "ru".equals(lang)
                ? Map.of(
                DayOfWeek.MONDAY, "понедельникам",
                DayOfWeek.TUESDAY, "вторникам",
                DayOfWeek.WEDNESDAY, "средам",
                DayOfWeek.THURSDAY, "четвергам",
                DayOfWeek.FRIDAY, "пятницам",
                DayOfWeek.SATURDAY, "субботам",
                DayOfWeek.SUNDAY, "воскресеньям")
                : Map.of(
                DayOfWeek.MONDAY, "Mondays",
                DayOfWeek.TUESDAY, "Tuesdays",
                DayOfWeek.WEDNESDAY, "Wednesdays",
                DayOfWeek.THURSDAY, "Thursdays",
                DayOfWeek.FRIDAY, "Fridays",
                DayOfWeek.SATURDAY, "Saturdays",
                DayOfWeek.SUNDAY, "Sundays");

        List<String> names = sorted.stream()
                .map(d -> map.getOrDefault(d, d.toString().toLowerCase() + "s"))
                .toList();

        if (names.size() == 1) return names.getFirst();
        String last = names.getLast();
        return String.join(", ", names.subList(0, names.size() - 1)) +
                ("ru".equals(lang) ? " и " : " and ") + last;
    }

    private Instant parseDate(String dateStr, String context) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        try {
            return LocalDateTime.parse(dateStr, formatter).atZone(timeZone).toInstant();
        } catch (DateTimeParseException e) {
            plugin.getLogger().warning("Invalid date format for " + context + ": " + dateStr + ". Using default.");
            try {
                return LocalDateTime.parse(DEFAULT_DATE, formatter).atZone(timeZone).toInstant();
            } catch (DateTimeParseException ex) {
                return Instant.MAX;
            }
        }
    }

    public boolean isDisableNether() {
        return disableNether;
    }

    public boolean isDisableEnd() {
        return disableEnd;
    }

    public boolean isDisableElytra() {
        return disableElytra;
    }

    public boolean isDisableNetherPortalCreation() {
        return disableNetherPortalCreation;
    }

    public boolean isDisableNetherTeleportation() {
        return disableNetherTeleportation;
    }

    public boolean isDisableEndPortalActivation() {
        return disableEndPortalActivation;
    }

    public boolean isDisableElytraEquip() {
        return disableElytraEquip;
    }

    public boolean isDisableElytraFlight() {
        return disableElytraFlight;
    }

    public Instant getNetherRestrictionInstant() {
        return netherRestrictionInstant;
    }

    public Instant getEndRestrictionInstant() {
        return endRestrictionInstant;
    }

    public Instant getElytraRestrictionInstant() {
        return elytraRestrictionInstant;
    }

    public ZoneId getTimeZone() {
        return timeZone;
    }

    public Set<String> getCustomWorlds() {
        return customWorldDisables.keySet();
    }

    public boolean isCustomWorldDisabled(String worldName) {
        return customWorldDisables.getOrDefault(worldName, false);
    }

    public Instant getCustomWorldRestrictionInstant(String worldName) {
        return customWorldRestrictionInstants.getOrDefault(worldName, Instant.MAX);
    }

    public String getNotificationMode() {
        return notificationMode;
    }

    public boolean isAdminAlert() {
        return proEdition && adminAlert;
    }

    public boolean isNotificationBossbar() {
        return proEdition && notificationBossbar;
    }

    public String getNotificationSound() {
        return notificationSound;
    }

    public boolean isUseCustomSpawn() {
        return proEdition && useCustomSpawn;
    }

    public Location getCustomSpawnLocation() {
        if (!isUseCustomSpawn()) return null;
        var world = Bukkit.getWorld(customSpawnWorld);
        if (world == null) return null;
        return new Location(world, customSpawnX, customSpawnY, customSpawnZ, (float) customSpawnYaw, (float) customSpawnPitch);
    }

    public boolean isUpdateCheckEnabled() {
        return updateCheckEnabled;
    }

    public String getModrinthId() {
        return modrinthId;
    }

    public boolean isAutoCleanBypasses() {
        return autoCleanBypasses;
    }

    public boolean isProEdition() {
        return proEdition;
    }

    public boolean isDiscordEnabled() {
        return discordEnabled && discordWebhook != null && !discordWebhook.isBlank();
    }

    public String getDiscordWebhook() {
        return discordWebhook;
    }

    public boolean isDiscordSendBlock() {
        return discordSendBlock;
    }

    public boolean isDiscordSendUpdate() {
        return discordSendUpdate;
    }

    public boolean isGuiEnabled() {
        return guiEnabled;
    }

    public boolean isStatsEnabled() {
        return statsEnabled;
    }

    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    public String getSyncChannel() {
        return syncChannel;
    }
}
