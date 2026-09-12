package com.dafealru.smartlogin.gui;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.PasswordHasher;
import com.dafealru.smartlogin.database.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PinPadGUI implements Listener {

    private final SmartLogin plugin;
    private final Map<UUID, StringBuilder> enteredPins = new HashMap<>();

    public PinPadGUI(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public void openPinPad(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("🔒 Virtual Security PIN Pad", NamedTextColor.GOLD, TextDecoration.BOLD));

        List<Integer> digits = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 0));
        if (plugin.getModularConfig().getPinpadConfig().getBoolean("randomize-positions", true)) {
            Collections.shuffle(digits);
        }

        int[] slots = {12, 13, 14, 21, 22, 23, 30, 31, 32, 40};
        for (int i = 0; i < slots.length; i++) {
            inv.setItem(slots[i], createDigitItem(digits.get(i)));
        }

        // Clear button
        ItemStack clear = new ItemStack(Material.RED_CONCRETE);
        ItemMeta clearMeta = clear.getItemMeta();
        clearMeta.displayName(Component.text("✖ CLEAR PIN", NamedTextColor.RED, TextDecoration.BOLD));
        clear.setItemMeta(clearMeta);
        inv.setItem(39, clear);

        // Submit button
        ItemStack submit = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta submitMeta = submit.getItemMeta();
        submitMeta.displayName(Component.text("✔ SUBMIT / LOGIN", NamedTextColor.GREEN, TextDecoration.BOLD));
        submit.setItemMeta(submitMeta);
        inv.setItem(41, submit);

        enteredPins.put(player.getUniqueId(), new StringBuilder());
        player.openInventory(inv);
    }

    private ItemStack createDigitItem(int digit) {
        ItemStack item = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("[ " + digit + " ]", NamedTextColor.AQUA, TextDecoration.BOLD));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(Component.text("🔒 Virtual Security PIN Pad", NamedTextColor.GOLD, TextDecoration.BOLD))) return;

        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        StringBuilder pin = enteredPins.computeIfAbsent(player.getUniqueId(), k -> new StringBuilder());
        String name = item.getItemMeta().getDisplayName();

        if (item.getType() == Material.LIGHT_BLUE_STAINED_GLASS_PANE) {
            String title = item.getItemMeta().displayName().toString();
            for (int d = 0; d <= 9; d++) {
                if (title.contains(String.valueOf(d))) {
                    pin.append(d);
                    player.sendMessage(Component.text("•", NamedTextColor.YELLOW));
                    break;
                }
            }
        } else if (item.getType() == Material.RED_CONCRETE) {
            pin.setLength(0);
            player.sendMessage(Component.text("PIN cleared.", NamedTextColor.GRAY));
        } else if (item.getType() == Material.LIME_CONCRETE) {
            String entered = pin.toString();
            PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
            if (profile != null && PasswordHasher.verify(entered, profile.getSalt(), profile.getPasswordHash())) {
                plugin.getAuthManager().setAuthenticated(player.getUniqueId(), true);
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                player.removePotionEffect(PotionEffectType.SLOWNESS);
                plugin.getSpawnManager().handleLoginRestore(player);
                player.closeInventory();
                player.sendMessage(plugin.getLocaleManager().getComponent("success-logged-in", player));
            } else {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-wrong-password", player));
                pin.setLength(0);
            }
        }
    }
}
