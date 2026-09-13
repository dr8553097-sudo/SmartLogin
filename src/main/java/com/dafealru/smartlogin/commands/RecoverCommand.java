package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.recovery.AccountRecoveryManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RecoverCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public RecoverCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLocaleManager().parse("<red>Este comando solo puede ser usado por jugadores en el servidor.</red>"));
            return true;
        }

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("request"))) {
            boolean sent = plugin.getAccountRecoveryManager().requestRecovery(player);
            if (sent) {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Recovery</bold></gradient> <dark_gray>»</dark_gray> <green>¡Código OTP de 6 dígitos enviado a tu Discord/Telegram vinculado! Usa <yellow>/recover <código> <nueva_clave></yellow> para restablecer tu cuenta.</green>"));
            } else {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Recovery</bold></gradient> <dark_gray>»</dark_gray> <red>No tienes una cuenta de Telegram o Discord vinculada para recuperar tu contraseña. Contacta con un administrador.</red>"));
            }
            return true;
        }

        if (args.length >= 2) {
            String code = args[0];
            String newPassword = args[1];

            var result = plugin.getAccountRecoveryManager().resetPassword(player, code, newPassword);
            switch (result) {
                case SUCCESS -> {
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Recovery</bold></gradient> <dark_gray>»</dark_gray> <green>¡Tu contraseña ha sido restablecida con éxito con máxima seguridad Argon2id! Ya puedes iniciar sesión.</green>"));
                }
                case INVALID_CODE -> {
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Recovery</bold></gradient> <dark_gray>»</dark_gray> <red>El código OTP ingresado es incorrecto.</red>"));
                }
                case EXPIRED -> {
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Recovery</bold></gradient> <dark_gray>»</dark_gray> <red>El código OTP ha expirado. Solicita uno nuevo con /recover request.</red>"));
                }
                case WEAK_PASSWORD -> {
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Recovery</bold></gradient> <dark_gray>»</dark_gray> <red>La nueva contraseña no cumple los requisitos de seguridad o contiene tu nombre de usuario.</red>"));
                }
                case NO_SESSION -> {
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Recovery</bold></gradient> <dark_gray>»</dark_gray> <red>No tienes ninguna sesión de recuperación activa. Escribe /recover request para solicitar un código.</red>"));
                }
                case DATABASE_ERROR -> {
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Recovery</bold></gradient> <dark_gray>»</dark_gray> <red>Error interno de base de datos al restablecer la contraseña.</red>"));
                }
            }
            return true;
        }

        player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Recovery</bold></gradient> <dark_gray>»</dark_gray> <yellow>Uso: /recover request o /recover <código> <nueva_clave></yellow>"));
        return true;
    }
}
