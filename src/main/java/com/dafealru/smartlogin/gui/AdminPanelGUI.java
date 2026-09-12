package com.dafealru.smartlogin.gui;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
        Inventory inv = Bukkit.createInventory(null, 54, plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>⚡ SmartLogin Master Panel</bold></gradient>"));

        ItemStack purpleGlass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = purpleGlass.getItemMeta();
        glassMeta.displayName(Component.empty());
        purpleGlass.setItemMeta(glassMeta);

        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        blackGlass.setItemMeta(glassMeta);

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, purpleGlass);
            } else {
                inv.setItem(i, blackGlass);
            }
        }

        FileConfiguration config = plugin.getModularConfig().getConfig();
        FileConfiguration auth = plugin.getModularConfig().getAuthConfig();
        FileConfiguration totp = plugin.getModularConfig().getTotpConfig();
        FileConfiguration discord = plugin.getModularConfig().getDiscordConfig();
        FileConfiguration telegram = plugin.getModularConfig().getTelegramConfig();

        // 1. Stats Icon (Slot 4)
        ItemStack stats = new ItemStack(Material.NETHER_STAR);
        ItemMeta statsMeta = stats.getItemMeta();
        statsMeta.displayName(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>📊 Estadísticas del Sistema</bold></gradient>"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(plugin.getLocaleManager().parse(" <gray>• Versión:</gray> <#C084FC>v" + plugin.getPluginMeta().getVersion() + "</#C084FC>"));
        lore.add(plugin.getLocaleManager().parse(" <gray>• Base de Datos:</gray> <#C084FC>" + plugin.getModularConfig().getDatabaseConfig().getString("type", "SQLITE") + "</#C084FC>"));
        lore.add(plugin.getLocaleManager().parse(" <gray>• Idioma Auto:</gray> " + (config.getBoolean("general.auto-detect-client-language", true) ? "<green>✔ Activado</green>" : "<red>✖ Desactivado</red>")));
        lore.add(plugin.getLocaleManager().parse(" <gray>• SessionShield:</gray> " + (auth.getBoolean("session-shield.enabled", true) ? "<green>✔ Activo</green>" : "<red>✖ Inactivo</red>")));
        lore.add(Component.empty());
        statsMeta.lore(lore);
        stats.setItemMeta(statsMeta);
        inv.setItem(4, stats);

        // 2. Feature Toggles
        inv.setItem(20, createToggleItem(Material.TOTEM_OF_UNDYING, "2FA Google Authenticator", totp.getBoolean("enabled", true)));
        inv.setItem(21, createToggleItem(Material.ENDER_EYE, "Discord Webhook / Bot", discord.getBoolean("bot.enabled", false)));
        inv.setItem(22, createToggleItem(Material.PAPER, "Telegram Bot Alertas", telegram.getBoolean("enabled", false)));
        inv.setItem(23, createToggleItem(Material.BEDROCK, "Bedrock Floodgate Auto-Login", auth.getBoolean("bedrock.auto-login-enabled", true)));
        inv.setItem(24, createToggleItem(Material.GOLD_INGOT, "Mojang Premium Auto-Login", auth.getBoolean("premium.auto-login-enabled", true)));
        inv.setItem(29, createToggleItem(Material.SHIELD, "SessionShield Reconexión", auth.getBoolean("session-shield.enabled", true)));
        inv.setItem(30, createToggleItem(Material.NAME_TAG, "Protección Nick Strict-Case", config.getBoolean("nickname-protection.strict-case", true)));
        inv.setItem(31, createToggleItem(Material.BLAZE_POWDER, "Anti-Bot Captcha Dinámico", config.getInt("captcha.trigger-after-failed-attempts", 2) > 0));
        inv.setItem(32, createToggleItem(Material.COMPASS, "Geo-IP Detección de País", config.getBoolean("geo-protection.enabled", true)));
        inv.setItem(33, createToggleItem(Material.BEACON, "Limbo / Spawn Auth", config.getBoolean("auth-spawn.enabled", false)));

        // 3. Action Buttons
        ItemStack backup = new ItemStack(Material.CHEST);
        ItemMeta bMeta = backup.getItemMeta();
        bMeta.displayName(plugin.getLocaleManager().parse("<gradient:#A855F7:#C084FC><bold>💾 Crear Backup Instantáneo</bold></gradient>"));
        List<Component> bLore = new ArrayList<>();
        bLore.add(Component.text("Haz clic para crear un snapshot completo de la DB.", NamedTextColor.GRAY));
        bMeta.lore(bLore);
        backup.setItemMeta(bMeta);
        inv.setItem(48, backup);

        ItemStack reload = new ItemStack(Material.REDSTONE);
        ItemMeta rMeta = reload.getItemMeta();
        rMeta.displayName(plugin.getLocaleManager().parse("<gradient:#7C3AED:#9333EA><bold>🔄 Recargar Configuraciones & Idiomas</bold></gradient>"));
        List<Component> rLore = new ArrayList<>();
        rLore.add(Component.text("Haz clic para hot-reload de configs y mensajes.", NamedTextColor.GRAY));
        rMeta.lore(rLore);
        reload.setItemMeta(rMeta);
        inv.setItem(50, reload);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.8f);
    }

    private ItemStack createToggleItem(Material iconMat, String name, boolean state) {
        ItemStack item = new ItemStack(iconMat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getLocaleManager().parse((state ? "<green>✔ [ON] " : "<red>✖ [OFF] ") + "<#E9D5FF><bold>" + name + "</bold></#E9D5FF>"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(plugin.getLocaleManager().parse(" <gray>Estado actual:</gray> " + (state ? "<green><bold>HABILITADO</bold></green>" : "<red><bold>DESHABILITADO</bold></red>")));
        lore.add(plugin.getLocaleManager().parse(" <#C084FC>👉 Haz clic para alternar</#C084FC>"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>⚡ SmartLogin Master Panel</bold></gradient>"))) return;

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == 48) {
            File backup = plugin.getAuditManager().createDatabaseBackup();
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green>✔ Copia de seguridad creada: <yellow>" + (backup != null ? backup.getName() : "N/A") + "</yellow></green>"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            openPanel(player);
        } else if (slot == 50) {
            plugin.getModularConfig().loadAll();
            plugin.getLocaleManager().loadLanguages();
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green>✔ Todas las configuraciones e idiomas han sido recargados.</green>"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            openPanel(player);
        } else if (slot == 20) {
            plugin.getSetupWizardManager().handleToggle(player, "2fa");
            openPanel(player);
        } else if (slot == 23) {
            plugin.getSetupWizardManager().handleToggle(player, "bedrock");
            openPanel(player);
        } else if (slot == 24) {
            plugin.getSetupWizardManager().handleToggle(player, "premium");
            openPanel(player);
        }
    }
}
