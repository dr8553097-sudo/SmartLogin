package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.TotpEngine;
import com.dafealru.smartlogin.database.PlayerProfile;
import com.dafealru.smartlogin.twofactor.BackupCodeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TwoFactorCommand implements CommandExecutor {

    private final SmartLogin plugin;
    private final java.util.Map<java.util.UUID, Integer> failedAttempts = new java.util.concurrent.ConcurrentHashMap<>();

    public TwoFactorCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Solo los jugadores pueden usar 2FA.</#F5D0FE>"));
            return true;
        }

        if (args.length == 0) {
            String lang = plugin.getLocaleManager().getPlayerLanguage(player);
            boolean isEn = "en".equalsIgnoreCase(lang);

            player.sendMessage(Component.empty());
            player.sendMessage(plugin.getLocaleManager().parse(isEn ? "<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ SMARTLOGIN — 2FA SUITE ] ━━━━━━━━━━━━</bold></gradient>" : "<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ SMARTLOGIN — SUITE 2FA ] ━━━━━━━━━━━━</bold></gradient>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <#C084FC><bold>/2fa setup</bold></#C084FC> <dark_gray>—</dark_gray> <gray>" + (isEn ? "Receive QR map and emergency backup codes" : "Recibe mapa QR de Google Auth y códigos de respaldo") + "</gray>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <#C084FC><bold>/2fa verify <code></bold></#C084FC> <dark_gray>—</dark_gray> <gray>" + (isEn ? "Verify 6-digit Google Authenticator code" : "Verifica el código de 6 dígitos") + "</gray>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <#C084FC><bold>/2fa recovery <code></bold></#C084FC> <dark_gray>—</dark_gray> <gray>" + (isEn ? "Use one-time backup recovery code" : "Usa un código de respaldo de emergencia") + "</gray>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <#C084FC><bold>/2fa disable <code></bold></#C084FC> <dark_gray>—</dark_gray> <gray>" + (isEn ? "Disable 2FA security protection" : "Desactiva la protección 2FA") + "</gray>"));
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
            player.sendMessage(Component.empty());
            return true;
        }

        String sub = args[0].toLowerCase();
        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
        if (profile == null) {
            profile = plugin.getDatabaseManager().loadProfile(player.getUniqueId()).join();
            if (profile != null) {
                plugin.getAuthManager().cacheProfile(player.getUniqueId(), profile);
            }
        }

        // Direct code support: /2fa 123456 or /2fa 123 456 or /2fa 123-456
        String rawJoined = String.join("", args).replaceAll("[^0-9]", "");
        if (rawJoined.length() == 6 && !sub.equals("setup") && !sub.equals("disable") && !sub.equals("recovery") && !sub.equals("help")) {
            return handleVerify(player, profile, rawJoined);
        }

        if (sub.equals("setup")) {
            if (profile == null || profile.getPasswordHash() == null) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-not-registered", player));
                return true;
            }

            List<String> recoveryCodes = plugin.getTwoFactorManager().setupNew2FA(profile);
            plugin.getDatabaseManager().saveProfile(profile);
            plugin.getAuthManager().cacheProfile(player.getUniqueId(), profile);
            plugin.getQrMapManager().giveQrMap(player, profile.getTotpSecret());

            player.sendMessage(Component.empty());
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ 🔐 VINCULACIÓN DE GOOGLE AUTHENTICATOR ] ━━━━━━━━━━━━</bold></gradient>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF><bold>Paso 1:</bold> Escanea el mapa QR en tu mano con la app <#C084FC>Google Authenticator</#C084FC> (o Authy/Aegis).</#E9D5FF>"));

            Component copyBtn = plugin.getLocaleManager().parse("    <gray>¿No puedes escanear?</gray> <#C084FC><bold>" + profile.getTotpSecret() + "</bold></#C084FC> <yellow>(Clic para copiar clave)</yellow>")
                    .clickEvent(ClickEvent.copyToClipboard(profile.getTotpSecret()))
                    .hoverEvent(HoverEvent.showText(Component.text("Haz clic para copiar la clave secreta manual", NamedTextColor.LIGHT_PURPLE)));
            player.sendMessage(copyBtn);

            player.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF><bold>Paso 2:</bold> Tu app generará un código de 6 dígitos que cambia cada 30s.</#E9D5FF>"));

            Component verifyClick = plugin.getLocaleManager().parse("  <#E9D5FF><bold>Paso 3:</bold> Escribe en el chat: <#C084FC><bold>/2fa verify <código></bold></#C084FC> <yellow>(Clic para autocompletar)</yellow></#E9D5FF>")
                    .clickEvent(ClickEvent.suggestCommand("/2fa verify "))
                    .hoverEvent(HoverEvent.showText(Component.text("Haz clic para autocompletar /2fa verify en el chat", NamedTextColor.LIGHT_PURPLE)));
            player.sendMessage(verifyClick);

            player.sendMessage(Component.empty());
            player.sendMessage(plugin.getLocaleManager().parse("  <#F5D0FE><bold>🛡️ CÓDIGOS DE RESPALDO DE EMERGENCIA (Guárdalos por si pierdes tu celular):</bold></#F5D0FE>"));
            for (String code : recoveryCodes) {
                player.sendMessage(plugin.getLocaleManager().parse("    <#C084FC>➤</#C084FC> <#F5D0FE><bold>" + code + "</bold></#F5D0FE>"));
            }
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
            player.sendMessage(Component.empty());
            return true;
        }

        if (sub.equals("verify")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /2fa verify <código de 6 dígitos></#F5D0FE>"));
                return true;
            }
            String codeInput = String.join("", java.util.Arrays.copyOfRange(args, 1, args.length));
            return handleVerify(player, profile, codeInput);
        }

        if (sub.equals("recovery")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /2fa recovery <código de respaldo></#F5D0FE>"));
                return true;
            }
            if (profile == null || !profile.is2FAEnabled()) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-not-setup", player));
                return true;
            }

            if (BackupCodeManager.verifyAndConsume(profile, args[1])) {
                plugin.getDatabaseManager().saveProfile(profile);
                plugin.getAuthManager().cacheProfile(player.getUniqueId(), profile);
                plugin.getQrMapManager().removeQrMap(player);
                plugin.getAuthManager().completeAuthentication(player, null);
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ ¡Código de recuperación aceptado! Autenticación de emergencia correcta.</#E9D5FF>"));
            } else {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>✖ Código de recuperación inválido o ya consumido.</#F5D0FE>"));
            }
            return true;
        }

        if (sub.equals("disable")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /2fa disable <código></#F5D0FE>"));
                return true;
            }
            if (profile == null || !profile.is2FAEnabled()) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-not-setup", player));
                return true;
            }

            String cleaned = args[1].replaceAll("[^0-9]", "").trim();
            try {
                int code = Integer.parseInt(cleaned);
                if (TotpEngine.verifyCode(profile.getTotpSecret(), code, 3)) {
                    profile.set2FAEnabled(false);
                    profile.setTotpSecret(null);
                    profile.setBackupCodes(null);
                    plugin.getDatabaseManager().saveProfile(profile);
                    plugin.getAuthManager().cacheProfile(player.getUniqueId(), profile);
                    plugin.getQrMapManager().removeQrMap(player);

                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin 2FA</bold></gradient> <dark_gray>»</dark_gray> <yellow>✔ Google Authenticator ha sido desvinculado y desactivado de tu cuenta.</yellow>"));
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                } else {
                    player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-invalid", player));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-invalid", player));
            }
            return true;
        }

        return true;
    }

    private boolean handleVerify(Player player, PlayerProfile profile, String codeStr) {
        if (profile == null || profile.getTotpSecret() == null) {
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>SmartLogin 2FA</bold></gradient> <dark_gray>»</dark_gray> <red>No has generado tu código QR. Escribe <#C084FC>/2fa setup</#C084FC> primero.</red>"));
            return true;
        }

        String cleaned = codeStr.replaceAll("[^0-9]", "").trim();
        if (cleaned.isEmpty()) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-invalid", player));
            return true;
        }

        try {
            int code = Integer.parseInt(cleaned);
            if (TotpEngine.verifyCode(profile.getTotpSecret(), code, 3)) {
                failedAttempts.remove(player.getUniqueId());
                boolean wasAlreadyEnabled = profile.is2FAEnabled();
                profile.set2FAEnabled(true);
                if (player.getAddress() != null) {
                    profile.setLastIp(player.getAddress().getAddress().getHostAddress());
                }
                profile.setLastLoginTimestamp(System.currentTimeMillis());
                plugin.getDatabaseManager().saveProfile(profile);
                plugin.getAuthManager().cacheProfile(player.getUniqueId(), profile);

                // Remove QR Map from inventory
                plugin.getQrMapManager().removeQrMap(player);

                if (!wasAlreadyEnabled) {
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin 2FA</bold></gradient> <dark_gray>»</dark_gray> <green>✔ ¡Google Authenticator ha sido vinculado y activado exitosamente en tu cuenta!</green>"));
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                } else {
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin 2FA</bold></gradient> <dark_gray>»</dark_gray> <green>✔ ¡Código 2FA verificado correctamente!</green>"));
                }

                if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                    plugin.getAuthManager().completeAuthentication(player, "success-logged-in");
                }
            } else {
                int fails = failedAttempts.getOrDefault(player.getUniqueId(), 0) + 1;
                failedAttempts.put(player.getUniqueId(), fails);
                if (fails >= 3) {
                    failedAttempts.remove(player.getUniqueId());
                    player.kick(plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>SmartLogin 2FA</bold></gradient>\n\n<red>Has superado el límite de 3 intentos de código 2FA.\nPor favor espera unos minutos antes de reintentar.</red>"));
                    plugin.getDiscordManager().sendWebhookAlert("🚨 [2FA] INTENTOS FALLIDOS", "El usuario **" + player.getName() + "** fue expulsado por fallar 3 veces consecutivas su código 2FA.", 0xEF4444);
                    return true;
                }
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>SmartLogin 2FA</bold></gradient> <dark_gray>»</dark_gray> <red>Código inválido. Intentos restantes: " + (3 - fails) + "/3</red>"));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-invalid", player));
        }
        return true;
    }
}
