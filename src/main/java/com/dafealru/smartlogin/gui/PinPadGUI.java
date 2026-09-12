package com.dafealru.smartlogin.gui;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PinPadGUI implements Listener {

    private final SmartLogin plugin;
    private final Map<UUID, StringBuilder> enteredPins = new HashMap<>();

    public PinPadGUI(SmartLogin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openPinPad(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, plugin.getLocaleManager().parse("<dark_purple><bold>Security PIN Pad</bold></dark_purple>"));
        enteredPins.put(player.getUniqueId(), new StringBuilder());

        // Fill background with black stained glass
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Layout numbers 1-9
        int[] numSlots = {12, 13, 14, 21, 22, 23, 30, 31, 32};
        for (int i = 0; i < 9; i++) {
            inv.setItem(numSlots[i], createItem(Material.PURPLE_CONCRETE, "<bold><white>" + (i + 1) + "</white></bold>"));
        }

        // Clear (Slot 39), Zero (Slot 40), Confirm (Slot 41)
        inv.setItem(39, createItem(Material.RED_CONCRETE, "<bold><red>✖ Clear</red></bold>"));
        inv.setItem(40, createItem(Material.PURPLE_CONCRETE, "<bold><white>0</white></bold>"));
        inv.setItem(41, createItem(Material.EMERALD_BLOCK, "<bold><green>✔ Confirm</green></bold>"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(plugin.getLocaleManager().parse("<dark_purple><bold>Security PIN Pad</bold></dark_purple>"))) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        StringBuilder pin = enteredPins.computeIfAbsent(player.getUniqueId(), k -> new StringBuilder());

        if (clicked.getType() == Material.RED_CONCRETE) {
            pin.setLength(0);
            player.sendMessage(plugin.getLocaleManager().getMessage("pin_pad_button_clear"));
            return;
        }

        if (clicked.getType() == Material.EMERALD_BLOCK) {
            String finalPin = pin.toString();
            player.closeInventory();
            player.performCommand("login " + finalPin);
            return;
        }

        if (clicked.getType() == Material.PURPLE_CONCRETE) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null) {
                String name = meta.getDisplayName();
                if (pin.length() < 8) {
                    pin.append(clicked.getAmount() > 0 ? "1" : "0"); // simple append
                    player.sendMessage(plugin.getLocaleManager().parse("<gray>PIN: <gold>" + "*".repeat(pin.length()) + "</gold></gray>"));
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        enteredPins.remove(event.getPlayer().getUniqueId());
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLocaleManager().parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
