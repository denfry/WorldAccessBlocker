package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
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

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_DATE = "2099-12-31 23:59:59";

    private final Map<String, Boolean> customWorldDisables = new HashMap<>();
    private final Map<String, Instant> customWorldRestrictionInstants = new HashMap<>();
    private final Map<String, List<SchedulePeriod>> recurringPeriods = new HashMap<>();
    private final Map<String, Boolean> useRecurring = new HashMap<>();

    private static class SchedulePeriod {
        List<DayOfWeek> days = new ArrayList<>();
        LocalTime start;
        LocalTime end;
    }

    public ConfigManager(WorldAccessBlocker plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public void loadConfigValues() {
        FileConfiguration config = plugin.getConfig();

        lang = config.getString("language", "en");

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
                try {
                    p.start = LocalTime.parse((String) startObj);
                    p.end = LocalTime.parse((String) endObj);

                    if (p.end.isBefore(p.start)) {
                        plugin.getLogger().warning("End time before start time for " + feature + " - skipping period");
                        continue;
                    }
                } catch (DateTimeParseException e) {
                    plugin.getLogger().warning("Invalid time format in " + feature + ": " + startObj + " - " + endObj);
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

    public boolean isRestrictionActive(String feature, Instant now) {
        if (!getDisable(feature)) return false;

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
                                            time.isBefore(p.end)));
        }

        Instant until = getRestrictionInstant(feature);
        return now.isBefore(until);
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT).withZone(timeZone);
        try {
            return ZonedDateTime.parse(dateStr, formatter).toInstant();
        } catch (DateTimeParseException e) {
            plugin.getLogger().warning("Invalid date format for " + context + ": " + dateStr + ". Using default.");
            try {
                return ZonedDateTime.parse(DEFAULT_DATE, formatter).toInstant();
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
}