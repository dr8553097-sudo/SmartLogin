package com.dafealru.smartlogin.captcha;

import com.dafealru.smartlogin.SmartLogin;
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

import java.util.*;

public class CaptchaManager implements Listener {

    private final SmartLogin plugin;
    private final Map<UUID, Material> requiredCaptchaItems = new HashMap<>();
    private final Set<UUID> pendingCaptcha = new HashSet<>();

    private static final Material[] TARGET_ITEMS = {
            Material.GOLDEN_APPLE,
            Material.DIAMOND,
            Material.EMERALD,
            Material.NETHERITE_INGOT,
            Material.ENDER_PEARL,
            Material.TOTEM_OF_UNDYING,
            Material.BLAZE_ROD
    };

    private static final Material[] FILLER_ITEMS = {
            Material.COAL,
            Material.STICK,
            Material.IRON_NUGGET,
            Material.FEATHER,
            Material.DIRT,
            Material.STONE,
            Material.OAK_PLANKS,
            Material.STRING,
            Material.BONE,
            Material.FLINT,
            Material.LEATHER
    };

    public CaptchaManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public boolean isPendingCaptcha(UUID uuid) {
        return pendingCaptcha.contains(uuid);
    }

    public void openCaptcha(Player player) {
        Random rand = new Random();
        Material target = TARGET_ITEMS[rand.nextInt(TARGET_ITEMS.length)];
        requiredCaptchaItems.put(player.getUniqueId(), target);
        pendingCaptcha.add(player.getUniqueId());

        String targetName = target.name().replace("_", " ").toLowerCase();
        Inventory inv = Bukkit.createInventory(null, 27,
                Component.text("🧩 Click the: " + targetName, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

        int targetSlot = rand.nextInt(27);
        for (int i = 0; i < 27; i++) {
            if (i == targetSlot) {
                inv.setItem(i, createItem(target, "✔ Click here to verify", NamedTextColor.GREEN));
            } else {
                Material filler = FILLER_ITEMS[rand.nextInt(FILLER_ITEMS.length)];
                inv.setItem(i, createItem(filler, "✖ Wrong Item", NamedTextColor.RED));
            }
        }

        player.openInventory(inv);
        player.sendMessage(Component.text("🧩 Captcha Challenge: Please click the [" + targetName + "] in your inventory to continue!", NamedTextColor.GOLD));
    }

    private ItemStack createItem(Material mat, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color, TextDecoration.BOLD));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!pendingCaptcha.contains(player.getUniqueId())) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        Material required = requiredCaptchaItems.get(player.getUniqueId());
        if (required != null && clicked.getType() == required) {
            pendingCaptcha.remove(player.getUniqueId());
            requiredCaptchaItems.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(Component.text("✔ Captcha verified successfully! You may now log in.", NamedTextColor.GREEN, TextDecoration.BOLD));
        } else {
            player.sendMessage(Component.text("✖ Incorrect item clicked! Refreshing captcha...", NamedTextColor.RED));
            openCaptcha(player);
        }
    }
}
