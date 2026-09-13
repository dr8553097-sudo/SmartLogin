package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EmailCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public EmailCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Solo los jugadores pueden gestionar su correo.</#F5D0FE>"));
            return true;
        }

        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
        if (profile == null || profile.getPasswordHash() == null) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-not-registered", player));
            return true;
        }

        if (args.length == 0) {
            boolean isConfigured = plugin.getEmailManager().isConfigured();
            String currentEmail = profile.getEmail() != null ? plugin.getEmailManager().maskEmail(profile.getEmail()) : "<red>No vinculado</red>";
            player.sendMessage(Component.empty());
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ 📧 SMARTLOGIN — GMAIL / EMAIL 2FA ] ━━━━━━━━━━━━</bold></gradient>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <gray>Correo actual:</gray> <#E9D5FF><bold>" + currentEmail + "</bold></#E9D5FF>"));
            if (!isConfigured) {
                player.sendMessage(plugin.getLocaleManager().parse("  <#EF4444>⚠ El servicio SMTP no está configurado en el servidor.</#EF4444>"));
                player.sendMessage(plugin.getLocaleManager().parse("  <gray>El admin debe configurar credenciales en </gray><yellow>plugins/SmartLogin/2fa/email.yml</yellow>"));
            } else {
                player.sendMessage(plugin.getLocaleManager().parse("  <#C084FC><bold>/email add <tu@correo.com></bold></#C084FC> <dark_gray>—</dark_gray> <gray>Envía código OTP a tu bandeja de entrada</gray>"));
                player.sendMessage(plugin.getLocaleManager().parse("  <#C084FC><bold>/email verify <código></bold></#C084FC> <dark_gray>—</dark_gray> <gray>Verifica el código y activa la protección</gray>"));
            }
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
            player.sendMessage(Component.empty());
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("verify")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Email</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /email verify <código de 6 dígitos></#F5D0FE>"));
                return true;
            }
            String code = args[1];
            boolean success = plugin.getEmailManager().completeVerification(player, code);
            if (success) {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Email</bold></gradient> <dark_gray>»</dark_gray> <green>✔ ¡Correo electrónico verificado y vinculado exitosamente con tu cuenta!</green>"));
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            } else {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Email</bold></gradient> <dark_gray>»</dark_gray> <red>Código de verificación inválido o expirado. Solicita uno nuevo con /email add <correo>.</red>"));
            }
            return true;
        }

        if (!plugin.getEmailManager().isConfigured()) {
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Email</bold></gradient> <dark_gray>»</dark_gray> <red>El servicio de correo SMTP no está configurado o está deshabilitado en el servidor. El administrador debe habilitarlo en <yellow>plugins/SmartLogin/2fa/email.yml</yellow>.</red>"));
            return true;
        }

        String targetEmail = sub.equals("add") ? (args.length >= 2 ? args[1] : null) : args[0];
        if (targetEmail == null || !plugin.getEmailManager().isValidEmail(targetEmail)) {
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Email</bold></gradient> <dark_gray>»</dark_gray> <red>Por favor ingresa un correo electrónico válido (ejemplo: /email add usuario@gmail.com).</red>"));
            return true;
        }

        boolean sent = plugin.getEmailManager().startVerification(player, targetEmail);
        if (sent) {
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Email</bold></gradient> <dark_gray>»</dark_gray> <green>¡Código OTP de 6 dígitos enviado a <yellow>" + plugin.getEmailManager().maskEmail(targetEmail) + "</yellow>! Revisa tu bandeja de entrada o spam y escribe <yellow>/email verify <código></yellow>.</green>"));
        } else {
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Email</bold></gradient> <dark_gray>»</dark_gray> <red>No se pudo enviar el correo. Revisa la consola del servidor o la configuración SMTP.</red>"));
        }
        return true;
    }
}
