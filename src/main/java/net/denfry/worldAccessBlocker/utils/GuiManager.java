package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiManager implements Listener {
    private final WorldAccessBlocker plugin;

    public GuiManager(WorldAccessBlocker plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text(plugin.getLanguageManager().getMessage("gui_title")));
        inv.setItem(1, toggleItem(Material.NETHERRACK, "Nether", plugin.getConfigManager().isDisableNether()));
        inv.setItem(3, toggleItem(Material.END_STONE, "End", plugin.getConfigManager().isDisableEnd()));
        inv.setItem(5, toggleItem(Material.ELYTRA, "Elytra", plugin.getConfigManager().isDisableElytra()));
        player.openInventory(inv);
    }

    private ItemStack toggleItem(Material material, String name, boolean state) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name + ": " + (state ? "Disabled" : "Enabled")));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        var titleComp = event.getView().title();
        String title = titleComp == null ? "" : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(titleComp);
        if (!title.toLowerCase().contains("wab")) return;
        event.setCancelled(true);
        switch (event.getRawSlot()) {
            case 1 -> toggle(player, "nether.disable", !plugin.getConfigManager().isDisableNether());
            case 3 -> toggle(player, "end.disable", !plugin.getConfigManager().isDisableEnd());
            case 5 -> toggle(player, "elytra.disable", !plugin.getConfigManager().isDisableElytra());
        }
    }

    private void toggle(Player player, String path, boolean value) {
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
        plugin.getConfigManager().loadConfigValues();
        open(player);
    }
}

