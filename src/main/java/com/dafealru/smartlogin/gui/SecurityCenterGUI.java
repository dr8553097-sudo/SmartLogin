package com.dafealru.smartlogin.gui;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Enterprise In-Game Security Center GUI (/security or /smartlogin profile).
 * Centerpiece for the 3 Pure 2FA Pillars: Google Authenticator, Discord 2FA, and Gmail 2FA.
 */
public class SecurityCenterGUI implements Listener {

    private final SmartLogin plugin;

    public SecurityCenterGUI(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        openSecurityCenter(player);
    }

    public void openSecurityCenter(Player player) {
        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-not-logged-in", player));
            return;
        }

        PlayerProfile cached = plugin.getAuthManager().getProfile(player.getUniqueId());
        if (cached != null) {
            renderGUI(player, cached);
            return;
        }

        plugin.getDatabaseManager().loadProfile(player.getUniqueId()).thenAccept(profile -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline() || profile == null) return;
                renderGUI(player, profile);
            });
        });
    }

    private void renderGUI(Player player, PlayerProfile profile) {
        Inventory inv = Bukkit.createInventory(null, 45, plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>🛡️ Centro de Seguridad</bold></gradient>"));

        // Fill background
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.displayName(Component.empty());
        bg.setItemMeta(bgMeta);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        // Calculate Security Score
        int score = 25; // Base
        if (profile.is2FAEnabled()) score += 35;
        if (profile.getDiscordId() != null && !profile.getDiscordId().isEmpty()) score += 20;
        if (profile.getEmail() != null && !profile.getEmail().isEmpty()) score += 20;
        score = Math.min(100, score);

        String scoreColor = score >= 80 ? "<green>" : score >= 50 ? "<yellow>" : "<red>";

        // Slot 13: Player Profile Card
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.displayName(plugin.getLocaleManager().parse("<gradient:#C084FC:#F5D0FE><bold>" + player.getName() + "</bold></gradient>"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String lastLogin = profile.getLastLoginTimestamp() > 0 ? sdf.format(new Date(profile.getLastLoginTimestamp())) : "N/A";

        List<Component> headLore = new ArrayList<>();
        headLore.add(Component.empty());
        headLore.add(plugin.getLocaleManager().parse("<gray>Nivel de Seguridad:</gray> " + scoreColor + "<bold>" + score + "% / 100%</bold></color>"));
        headLore.add(plugin.getLocaleManager().parse("<gray>Último acceso:</gray> <#E9D5FF>" + lastLogin + "</#E9D5FF>"));
        headLore.add(plugin.getLocaleManager().parse("<gray>Tipo de cuenta:</gray> " + (profile.isPremium() ? "<green>Mojang Premium</green>" : "<yellow>No-Premium</yellow>")));
        skullMeta.lore(headLore);
        head.setItemMeta(skullMeta);
        inv.setItem(13, head);

        // Slot 19: 2FA Pillar 1 — Google Authenticator (TOTP)
        ItemStack totpItem = new ItemStack(profile.is2FAEnabled() ? Material.LIME_BANNER : Material.SHIELD);
        ItemMeta totpMeta = totpItem.getItemMeta();
        totpMeta.displayName(plugin.getLocaleManager().parse("<gradient:#A855F7:#C084FC><bold>🔐 1. Google Authenticator (TOTP)</bold></gradient>"));
        List<Component> totpLore = new ArrayList<>();
        totpLore.add(Component.empty());
        totpLore.add(plugin.getLocaleManager().parse("<gray>Estado actual:</gray> " + (profile.is2FAEnabled() ? "<green><bold>✔ ACTIVADO</bold></green>" : "<red><bold>✖ DESACTIVADO</bold></red>")));
        totpLore.add(plugin.getLocaleManager().parse("<gray>Apps compatibles:</gray> <#E9D5FF>Google Auth, Authy, Aegis</#E9D5FF>"));
        totpLore.add(Component.empty());
        totpLore.add(plugin.getLocaleManager().parse(profile.is2FAEnabled() ? "<yellow>Clic para ver opciones / desactivar</yellow>" : "<green>Clic para configurar con mapa QR en mano</green>"));
        totpMeta.lore(totpLore);
        totpItem.setItemMeta(totpMeta);
        inv.setItem(19, totpItem);

        // Slot 21: 2FA Pillar 2 — Discord 2FA Suite
        boolean hasDiscord = profile.getDiscordId() != null && !profile.getDiscordId().isEmpty();
        boolean streamer = plugin.getStreamerManager() != null && plugin.getStreamerManager().isStreamerMode(player);
        ItemStack discordItem = new ItemStack(hasDiscord ? Material.HEART_OF_THE_SEA : Material.ENDER_EYE);
        ItemMeta discordMeta = discordItem.getItemMeta();
        discordMeta.displayName(plugin.getLocaleManager().parse("<gradient:#5865F2:#818CF8><bold>🤖 2. Discord 2FA & Link</bold></gradient>"));
        List<Component> discordLore = new ArrayList<>();
        discordLore.add(Component.empty());
        discordLore.add(plugin.getLocaleManager().parse("<gray>Estado vinculación:</gray> " + (hasDiscord ? "<green><bold>✔ VINCULADO</bold></green>" : "<red><bold>✖ NO VINCULADO</bold></red>")));
        if (hasDiscord) {
            String displayDiscord = streamer ? plugin.getStreamerManager().maskId(profile.getDiscordId()) : profile.getDiscordId();
            discordLore.add(plugin.getLocaleManager().parse("<gray>ID de Discord:</gray> <#818CF8>" + displayDiscord + "</#818CF8>"));
            discordLore.add(plugin.getLocaleManager().parse("<gray>Protección:</gray> <green>Aprobación por DM en nueva IP</green>"));
        } else {
            discordLore.add(plugin.getLocaleManager().parse("<gray>Protección:</gray> <yellow>Vincula para 2FA por bot</yellow>"));
        }
        discordLore.add(Component.empty());
        discordLore.add(plugin.getLocaleManager().parse("<yellow>Clic para abrir vinculación con /link</yellow>"));
        discordMeta.lore(discordLore);
        discordItem.setItemMeta(discordMeta);
        inv.setItem(21, discordItem);

        // Slot 23: 2FA Pillar 3 — Gmail / Email 2FA Suite
        boolean hasEmail = profile.getEmail() != null && !profile.getEmail().isEmpty();
        ItemStack emailItem = new ItemStack(hasEmail ? Material.WRITABLE_BOOK : Material.BOOK);
        ItemMeta emailMeta = emailItem.getItemMeta();
        emailMeta.displayName(plugin.getLocaleManager().parse("<gradient:#EA4335:#FBBC05><bold>📧 3. Gmail / Email 2FA</bold></gradient>"));
        List<Component> emailLore = new ArrayList<>();
        emailLore.add(Component.empty());
        emailLore.add(plugin.getLocaleManager().parse("<gray>Estado correo:</gray> " + (hasEmail ? "<green><bold>✔ VERIFICADO</bold></green>" : "<red><bold>✖ NO CONFIGURADO</bold></red>")));
        if (hasEmail) {
            String displayEmail = streamer ? plugin.getStreamerManager().maskEmail(profile.getEmail()) : plugin.getEmailManager().maskEmail(profile.getEmail());
            emailLore.add(plugin.getLocaleManager().parse("<gray>Correo vinculado:</gray> <#FBBC05>" + displayEmail + "</#FBBC05>"));
            emailLore.add(plugin.getLocaleManager().parse("<gray>Seguridad:</gray> <green>Alertas y códigos OTP por Gmail</green>"));
        } else {
            emailLore.add(plugin.getLocaleManager().parse("<gray>Seguridad:</gray> <yellow>Protección de rescate ante pérdida</yellow>"));
        }
        emailLore.add(Component.empty());
        emailLore.add(plugin.getLocaleManager().parse("<yellow>Clic para gestionar tu correo (/email)</yellow>"));
        emailMeta.lore(emailLore);
        emailItem.setItemMeta(emailMeta);
        inv.setItem(23, emailItem);

        // Slot 25: Panic Button
        ItemStack panicItem = new ItemStack(Material.TNT_MINECART);
        ItemMeta panicMeta = panicItem.getItemMeta();
        panicMeta.displayName(plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>🚨 Botón de Pánico</bold></gradient>"));
        List<Component> panicLore = new ArrayList<>();
        panicLore.add(Component.empty());
        panicLore.add(plugin.getLocaleManager().parse("<gray>¿Sospechas que alguien tiene acceso?</gray>"));
        panicLore.add(plugin.getLocaleManager().parse("<red>Revoca sesiones y tokens guardados en tu IP.</red>"));
        panicLore.add(Component.empty());
        panicLore.add(plugin.getLocaleManager().parse("<red><bold>Clic para cerrar otras sesiones</bold></red>"));
        panicMeta.lore(panicLore);
        panicItem.setItemMeta(panicMeta);
        inv.setItem(25, panicItem);

        // Slot 30: Location & IP History Card
        ItemStack geoItem = new ItemStack(Material.COMPASS);
        ItemMeta geoMeta = geoItem.getItemMeta();
        geoMeta.displayName(plugin.getLocaleManager().parse("<gradient:#38BDF8:#818CF8><bold>🌍 Historial de Conexión</bold></gradient>"));
        List<Component> geoLore = new ArrayList<>();
        geoLore.add(Component.empty());
        String rawIp = profile.getLastIp() != null ? profile.getLastIp() : "127.0.0.1";
        String maskedIp = streamer ? "***.***.***.***" : (rawIp.contains(".") ? rawIp.replaceAll("\\.\\d+\\.\\d+$", ".***.***") : "127.***.***");
        geoLore.add(plugin.getLocaleManager().parse("<gray>Última IP detectada:</gray> <#38BDF8>" + maskedIp + "</#38BDF8>"));
        geoLore.add(plugin.getLocaleManager().parse("<gray>Fecha de acceso:</gray> <#E9D5FF>" + lastLogin + "</#E9D5FF>"));
        geoLore.add(plugin.getLocaleManager().parse("<gray>Protección GeoIP:</gray> <green>✔ Monitoreada</green>"));
        geoMeta.lore(geoLore);
        geoItem.setItemMeta(geoMeta);
        inv.setItem(30, geoItem);

        // Slot 32: SmartSession Token Card
        ItemStack sessionItem = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta sessionMeta = sessionItem.getItemMeta();
        sessionMeta.displayName(plugin.getLocaleManager().parse("<gradient:#10B981:#34D399><bold>🔑 SmartSession Pro</bold></gradient>"));
        List<Component> sessionLore = new ArrayList<>();
        sessionLore.add(Component.empty());
        boolean sessionValid = plugin.getSessionShield().isSessionValid(player.getUniqueId(), rawIp);
        sessionLore.add(plugin.getLocaleManager().parse("<gray>Reconexión rápida:</gray> " + (sessionValid ? "<green><bold>✔ ACTIVA</bold></green>" : "<yellow>Standby / Token Listo</yellow>")));
        sessionLore.add(plugin.getLocaleManager().parse("<gray>Validez de token:</gray> <#34D399>60 minutos</#34D399>"));
        sessionLore.add(plugin.getLocaleManager().parse("<gray>Seguridad:</gray> <green>Cifrado por hardware & IP</green>"));
        sessionMeta.lore(sessionLore);
        sessionItem.setItemMeta(sessionMeta);
        inv.setItem(32, sessionItem);

        // Slot 34: Streamer Mode Card
        ItemStack streamerItem = new ItemStack(streamer ? Material.ENDER_EYE : Material.OBSERVER);
        ItemMeta streamerMeta = streamerItem.getItemMeta();
        streamerMeta.displayName(plugin.getLocaleManager().parse("<gradient:#C084FC:#F5D0FE><bold>🎥 Modo Streamer Shield</bold></gradient>"));
        List<Component> streamerLore = new ArrayList<>();
        streamerLore.add(Component.empty());
        streamerLore.add(plugin.getLocaleManager().parse("<gray>Estado actual:</gray> " + (streamer ? "<green><bold>✔ ACTIVADO</bold></green>" : "<red><bold>✖ DESACTIVADO</bold></red>")));
        streamerLore.add(plugin.getLocaleManager().parse("<gray>Protección:</gray> <#E9D5FF>Oculta IPs, correos y tokens</#E9D5FF>"));
        streamerLore.add(plugin.getLocaleManager().parse("<gray>Chat Shield:</gray> <#E9D5FF>Filtra contraseñas sin barra</#E9D5FF>"));
        streamerLore.add(Component.empty());
        streamerLore.add(plugin.getLocaleManager().parse("<yellow>Clic para alternar Modo Streamer (/streamer)</yellow>"));
        streamerMeta.lore(streamerLore);
        streamerItem.setItemMeta(streamerMeta);
        inv.setItem(34, streamerItem);

        // Slot 40: Close GUI
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.displayName(Component.text("✖ Cerrar Menú", NamedTextColor.RED, TextDecoration.BOLD));
        closeItem.setItemMeta(closeMeta);
        inv.setItem(40, closeItem);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.2f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().title().equals(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>🛡️ Centro de Seguridad</bold></gradient>"))) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || !item.hasItemMeta()) return;

            int slot = event.getSlot();
            switch (slot) {
                case 19 -> {
                    player.closeInventory();
                    player.performCommand("2fa");
                }
                case 21 -> {
                    player.closeInventory();
                    player.performCommand("link");
                }
                case 23 -> {
                    player.closeInventory();
                    player.performCommand("email");
                }
                case 25 -> {
                    player.closeInventory();
                    plugin.getSessionShield().invalidateSession(player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green>✔ Se han revocado todas las sesiones anteriores y tokens de tu cuenta.</green>"));
                }
                case 34 -> {
                    boolean enabled = plugin.getStreamerManager().toggleStreamerMode(player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);
                    if (enabled) {
                        player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Streamer</bold></gradient> <dark_gray>»</dark_gray> <green>¡Modo Streamer <b>ACTIVADO</b>! Se ocultarán tus datos y se filtrarán contraseñas en chat.</green>"));
                    } else {
                        player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Streamer</bold></gradient> <dark_gray>»</dark_gray> <yellow>Modo Streamer <b>DESACTIVADO</b>.</yellow>"));
                    }
                    openSecurityCenter(player);
                }
                case 40 -> player.closeInventory();
            }
        }
    }
}
