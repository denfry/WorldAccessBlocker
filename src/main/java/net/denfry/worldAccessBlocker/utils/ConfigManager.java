package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ConfigManager {
    private final WorldAccessBlocker plugin;
    private boolean disableNether, disableEnd, disableElytra;
    private Date netherRestrictionDate, endRestrictionDate, elytraRestrictionDate;

    public ConfigManager(WorldAccessBlocker plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public void loadConfigValues() {
        FileConfiguration config = plugin.getConfig();
        disableNether = config.getBoolean("disable-nether", true);
        disableEnd = config.getBoolean("disable-end", true);
        disableElytra = config.getBoolean("disable-elytra", true);

        netherRestrictionDate = parseDate(config.getString("nether-restriction-date", "2025-03-10 00:00:00"));
        endRestrictionDate = parseDate(config.getString("end-restriction-date", "2025-04-15 00:00:00"));
        elytraRestrictionDate = parseDate(config.getString("elytra-restriction-date", "2025-05-01 00:00:00"));
    }

    private Date parseDate(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            return new Date();
        }
    }

    public boolean isDisableNether() {
        return disableNether;
    }

    public boolean isDisableEnd() {
        return disableEnd;
    }

    public boolean isDisableElytra() {
        return !disableElytra;
    }

    public Date getNetherRestrictionDate() {
        return netherRestrictionDate;
    }

    public Date getEndRestrictionDate() {
        return endRestrictionDate;
    }

    public Date getElytraRestrictionDate() {
        return elytraRestrictionDate;
    }
}
