package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.TotpEngine;
import com.dafealru.smartlogin.database.PlayerProfile;
import com.dafealru.smartlogin.twofactor.BackupCodeManager;
import net.kyori.adventure.text.Component;
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
            player.sendMessage(Component.empty());
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ SMARTLOGIN — SUITE 2FA ] ━━━━━━━━━━━━</bold></gradient>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <#C084FC><bold>/2fa setup</bold></#C084FC> <dark_gray>—</dark_gray> <gray>Recibe mapa QR de Google Auth y códigos de respaldo</gray>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <#C084FC><bold>/2fa verify <código></bold></#C084FC> <dark_gray>—</dark_gray> <gray>Verifica el código de 6 dígitos</gray>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <#C084FC><bold>/2fa recovery <código></bold></#C084FC> <dark_gray>—</dark_gray> <gray>Usa un código de respaldo de emergencia</gray>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <#C084FC><bold>/2fa disable <código></bold></#C084FC> <dark_gray>—</dark_gray> <gray>Desactiva la protección 2FA</gray>"));
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
            player.sendMessage(Component.empty());
            return true;
        }

        String sub = args[0].toLowerCase();
        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());

        if (sub.equals("setup")) {
            if (profile == null || profile.getPasswordHash() == null) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-not-registered", player));
                return true;
            }

            List<String> recoveryCodes = plugin.getTwoFactorManager().setupNew2FA(profile);
            plugin.getQrMapManager().giveQrMap(player, profile.getTotpSecret());

            player.sendMessage(Component.empty());
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ 🔐 CÓDIGOS DE RECUPERACIÓN 2FA ] ━━━━━━━━━━━━</bold></gradient>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>¡GUARDA ESTOS CÓDIGOS! Si pierdes tu celular usa <#C084FC>/2fa recovery <código></#C084FC>:</#E9D5FF>"));
            for (String code : recoveryCodes) {
                player.sendMessage(plugin.getLocaleManager().parse("    <#C084FC>➤</#C084FC> <#F5D0FE><bold>" + code + "</bold></#F5D0FE>"));
            }
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
            player.sendMessage(Component.empty());
            return true;
        }

        if (sub.equals("verify")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /2fa verify <código de 6 dígitos></#F5D0FE>"));
                return true;
            }
            if (profile == null || profile.getTotpSecret() == null) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-not-setup", player));
                return true;
            }

            try {
                int code = Integer.parseInt(args[1]);
                if (TotpEngine.verifyCode(profile.getTotpSecret(), code, 1)) {
                    profile.set2FAEnabled(true);
                    plugin.getDatabaseManager().saveProfile(profile);
                    plugin.getAuthManager().completeAuthentication(player, "success-2fa-enabled");
                } else {
                    player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-invalid", player));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-invalid", player));
            }
            return true;
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

            try {
                int code = Integer.parseInt(args[1]);
                if (TotpEngine.verifyCode(profile.getTotpSecret(), code, 1)) {
                    profile.set2FAEnabled(false);
                    profile.setTotpSecret(null);
                    profile.setBackupCodes(null);
                    plugin.getDatabaseManager().saveProfile(profile);
                    player.sendMessage(plugin.getLocaleManager().getComponent("success-2fa-disabled", player));
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
}
