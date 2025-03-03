package net.denfry.worldAccessBlocker;

import net.denfry.worldAccessBlocker.listeners.EndBlocker;
import net.denfry.worldAccessBlocker.listeners.PortalBlocker;
import net.denfry.worldAccessBlocker.listeners.ElytraBlocker;
import net.denfry.worldAccessBlocker.utils.ConfigManager;
import net.denfry.worldAccessBlocker.utils.ReloadCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Logger;

public class WorldAccessBlocker extends JavaPlugin {

    private final Logger log = getLogger();

    @Override
    public void onEnable() {
        log.info("⚙️ Инициализация WorldAccessBlocker...");

        ConfigManager configManager = new ConfigManager(this);
        configManager.loadConfigValues();

        log.info("📂 Конфигурация загружена:");
        log.info("  ├─ 🔥 Запрет входа в Ад: " + configManager.isDisableNether() + " (до " + configManager.getNetherRestrictionDate() + ")");
        log.info("  ├─ ⚫ Запрет входа в Энд: " + configManager.isDisableEnd() + " (до " + configManager.getEndRestrictionDate() + ")");
        log.info("  ├─ 🛑 Запрет элитр: " + configManager.isDisableElytra() + " (до " + configManager.getElytraRestrictionDate() + ")");

        log.info("🔗 Регистрация событий...");
        getServer().getPluginManager().registerEvents(new PortalBlocker(this, configManager), this);
        getServer().getPluginManager().registerEvents(new EndBlocker(configManager), this);
        getServer().getPluginManager().registerEvents(new ElytraBlocker(configManager), this);
        log.info("✅ Все обработчики событий зарегистрированы!");

        if (getCommand("wabreload") != null) {
            Objects.requireNonNull(getCommand("wabreload")).setExecutor(new ReloadCommand(this, configManager));
            log.info("📝 Команда /wabreload успешно зарегистрирована!");
        } else {
            log.severe("❌ Ошибка: команда /wabreload не зарегистрирована в plugin.yml!");
        }

        if (Bukkit.getVersion().contains("Folia")) {
            log.warning("⚠️ ВНИМАНИЕ: Сервер работает на Folia!");
            log.warning("⚠️ Отмена порталов через PlayerPortalEvent НЕ РАБОТАЕТ в Folia.");
            log.warning("⚠️ Возможны проблемы с блокировкой входа в Ад.");
        }

        log.info("✅ Плагин WorldAccessBlocker успешно загружен!");
    }
}
