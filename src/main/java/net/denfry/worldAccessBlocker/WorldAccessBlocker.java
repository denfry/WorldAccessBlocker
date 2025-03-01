package net.denfry.worldAccessBlocker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import java.util.logging.Logger;

public class WorldAccessBlocker extends JavaPlugin implements Listener {

    private Date netherRestrictionDate;
    private Date endRestrictionDate;
    private Date elytraRestrictionDate;
    private boolean disableNether;
    private boolean disableEnd;
    private boolean disableElytra;

    private final Logger log = getLogger();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        updateConfig();
        loadConfigValues();
        getServer().getPluginManager().registerEvents(this, this);


        if (getCommand("wabreload") != null) {
            Objects.requireNonNull(getCommand("wabreload")).setExecutor(new ReloadCommand(this));
        } else {
            log.severe("❌ Ошибка: команда /wabreload не зарегистрирована в plugin.yml!");
        }

        log.info("✅ Плагин WorldAccessBlocker активирован!");
        log.info("📋 Конфигурация загружена:");
        log.info("  ├─ 🔥 Запрет входа в Ад: " + disableNether + " (до " + formatDate(netherRestrictionDate) + ")");
        log.info("  ├─ ⚫ Запрет входа в Энд: " + disableEnd + " (до " + formatDate(endRestrictionDate) + ")");
        log.info("  ├─ 🛑 Запрет элитр: " + disableElytra + " (до " + formatDate(elytraRestrictionDate) + ")");

    }

    protected void loadConfigValues() {
        FileConfiguration config = getConfig();

        disableNether = config.getBoolean("disable-nether", true);
        disableEnd = config.getBoolean("disable-end", true);
        disableElytra = config.getBoolean("disable-elytra", true);

        netherRestrictionDate = parseDate("nether-restriction-date", config.getString("nether-restriction-date", "2025-03-10 00:00:00"));
        endRestrictionDate = parseDate("end-restriction-date", config.getString("end-restriction-date", "2025-04-15 00:00:00"));
        elytraRestrictionDate = parseDate("elytra-restriction-date", config.getString("elytra-restriction-date", "2025-05-01 00:00:00"));
    }

    private void updateConfig() {
        boolean changed = false;
        FileConfiguration config = getConfig();

        if (!config.contains("nether-restriction-date")) {
            config.set("nether-restriction-date", "2025-03-10 00:00:00");
            changed = true;
        }
        if (!config.contains("end-restriction-date")) {
            config.set("end-restriction-date", "2025-04-15 00:00:00");
            changed = true;
        }
        if (!config.contains("elytra-restriction-date")) {
            config.set("elytra-restriction-date", "2025-05-01 00:00:00");
            changed = true;
        }


        if (changed) {
            saveConfig();
            log.info("⚙️ Конфиг обновлён! Новые настройки загружены.");
        }
    }

    private Date parseDate(String key, String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            log.warning("⚠️ Ошибка в формате даты для '" + key + "': '" + dateStr + "'. Используется текущая дата!");
            return new Date();
        }
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    @EventHandler
    public void onPlayerInsertEye(PlayerInteractEvent event) {
        if (!disableEnd) return;
        if (new Date().after(endRestrictionDate)) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            Block block = event.getClickedBlock();
            if (block == null || block.getType() != Material.END_PORTAL_FRAME) return;

            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("🚫 Активация портала в Энд запрещена до " + formatDate(endRestrictionDate) + "!")
                    .color(TextColor.color(0xFF0000)));

        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!disableNether) return;
        if (new Date().after(netherRestrictionDate)) return;
        if (event.getTo().getWorld().getEnvironment() == World.Environment.NETHER) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("🔥 Вход в Ад запрещён до " + formatDate(netherRestrictionDate) + "!")
                    .color(TextColor.color(0xFF0000)));

        }
    }

    @EventHandler
    public void onElytraUse(PlayerToggleFlightEvent event) {
        if (!disableElytra) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        if (new Date().before(elytraRestrictionDate)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("🛑 Полёт на элитрах запрещён до " + formatDate(elytraRestrictionDate) + "!")
                    .color(TextColor.color(0xFF0000)));

        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!disableElytra) return;

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        if (new Date().before(elytraRestrictionDate)) {
            ItemStack chestplate = player.getInventory().getChestplate();
            if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                player.getInventory().setChestplate(null);
                player.getInventory().addItem(chestplate);
                player.sendMessage(Component.text("🔻 Элитры сняты!").color(TextColor.color(0xFF0000)));

            }
        }

    }
}
