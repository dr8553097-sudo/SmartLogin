package com.dafealru.smartlogin.gui;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AdminPanelGUI implements Listener {

    private final SmartLogin plugin;

    public AdminPanelGUI(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public void openPanel(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("⚡ SmartLogin Master Control Panel", NamedTextColor.GOLD, TextDecoration.BOLD));

        FileConfiguration config = plugin.getModularConfig().getConfig();
        FileConfiguration auth = plugin.getModularConfig().getAuthConfig();
        FileConfiguration totp = plugin.getModularConfig().getTotpConfig();
        FileConfiguration discord = plugin.getModularConfig().getDiscordConfig();
        FileConfiguration telegram = plugin.getModularConfig().getTelegramConfig();

        // 1. Stats Icon
        ItemStack stats = new ItemStack(Material.NETHER_STAR);
        ItemMeta statsMeta = stats.getItemMeta();
        statsMeta.displayName(Component.text("📊 SmartLogin Server Stats", NamedTextColor.AQUA, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("• Engine Version: v" + plugin.getPluginMeta().getVersion(), NamedTextColor.GRAY));
        lore.add(Component.text("• Database: " + plugin.getModularConfig().getDatabaseConfig().getString("type", "SQLITE"), NamedTextColor.GRAY));
        lore.add(Component.text("• Client Auto-Language: " + (config.getBoolean("general.auto-detect-client-language", true) ? "✔ ON" : "✖ OFF"), NamedTextColor.GRAY));
        statsMeta.lore(lore);
        stats.setItemMeta(statsMeta);
        inv.setItem(4, stats);

        // 2. Toggles
        inv.setItem(19, createToggleItem(Material.TOTEM_OF_UNDYING, "TOTP QR Map 2FA", totp.getBoolean("enabled", true), "2fa"));
        inv.setItem(20, createToggleItem(Material.BLUE_DYE, "Discord Bot 2FA", discord.getBoolean("bot.enabled", false), "discord"));
        inv.setItem(21, createToggleItem(Material.PAPER, "Telegram Bot 2FA", telegram.getBoolean("enabled", false), "telegram"));
        inv.setItem(22, createToggleItem(Material.BEDROCK, "Bedrock Auto-Login", auth.getBoolean("bedrock.auto-login-enabled", true), "bedrock"));
        inv.setItem(23, createToggleItem(Material.GOLD_INGOT, "Mojang Premium Auto-Login", auth.getBoolean("premium.auto-login-enabled", true), "premium"));
        inv.setItem(24, createToggleItem(Material.SHIELD, "SessionShield IP Cache", auth.getBoolean("session-shield.enabled", true), "sessionshield"));
        inv.setItem(25, createToggleItem(Material.PUFFERFISH, "Anti-Impostor Case Strict", config.getBoolean("nickname-protection.strict-case", true), "strictcase"));

        // 3. Action Buttons
        ItemStack backup = new ItemStack(Material.CHEST);
        ItemMeta bMeta = backup.getItemMeta();
        bMeta.displayName(Component.text("💾 Create Instant Database Backup", NamedTextColor.GREEN, TextDecoration.BOLD));
        backup.setItemMeta(bMeta);
        inv.setItem(40, backup);

        ItemStack reload = new ItemStack(Material.REDSTONE);
        ItemMeta rMeta = reload.getItemMeta();
        rMeta.displayName(Component.text("🔄 Reload All Configs & Locales", NamedTextColor.YELLOW, TextDecoration.BOLD));
        reload.setItemMeta(rMeta);
        inv.setItem(49, reload);

        player.openInventory(inv);
    }

    private ItemStack createToggleItem(Material mat, String name, boolean state, String featureKey) {
        ItemStack item = new ItemStack(state ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text((state ? "✔ [ON] " : "✖ [OFF] ") + name, state ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to toggle this feature live!", NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(Component.text("⚡ SmartLogin Master Control Panel", NamedTextColor.GOLD, TextDecoration.BOLD))) return;

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == 40) {
            File backup = plugin.getAuditManager().createDatabaseBackup();
            player.sendMessage(Component.text("✔ Database backup created successfully: " + (backup != null ? backup.getName() : "N/A"), NamedTextColor.GREEN));
            openPanel(player);
        } else if (slot == 49) {
            plugin.getModularConfig().loadAll();
            plugin.getLocaleManager().loadLanguages();
            player.sendMessage(Component.text("✔ All configurations and languages hot-reloaded!", NamedTextColor.GREEN));
            openPanel(player);
        } else if (slot == 19) {
            plugin.getSetupWizardManager().handleToggle(player, "2fa");
            openPanel(player);
        } else if (slot == 22) {
            plugin.getSetupWizardManager().handleToggle(player, "bedrock");
            openPanel(player);
        } else if (slot == 23) {
            plugin.getSetupWizardManager().handleToggle(player, "premium");
            openPanel(player);
        }
    }
}
