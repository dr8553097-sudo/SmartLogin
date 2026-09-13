package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UnlinkCommand implements CommandExecutor, TabCompleter {

    private final SmartLogin plugin;

    public UnlinkCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Solo los jugadores pueden desvincular cuentas.</#F5D0FE>"));
            return true;
        }

        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-not-logged-in", player));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("discord")) {
            if (profile.getDiscordId() == null || profile.getDiscordId().isEmpty()) {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Discord</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Tu cuenta no está vinculada a ningún usuario de Discord.</#F5D0FE>"));
                return true;
            }
            plugin.getDiscordManager().unlinkDiscord(player);
            return true;
        }

        String target = args[0].toLowerCase();
        if (target.equals("email") || target.equals("gmail") || target.equals("correo")) {
            if (profile.getEmail() == null || profile.getEmail().isEmpty()) {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#EA4335:#FBBC05><bold>SmartLogin Email</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>No tienes ningún correo vinculado.</#F5D0FE>"));
                return true;
            }
            profile.setEmail(null);
            plugin.getDatabaseManager().saveProfile(profile);
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#EA4335:#FBBC05><bold>SmartLogin Email</bold></gradient> <dark_gray>»</dark_gray> <yellow>✔ Tu correo electrónico ha sido desvinculado con éxito.</yellow>"));
            return true;
        }

        if (target.equals("2fa") || target.equals("totp") || target.equals("google")) {
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin 2FA</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Para desactivar Google Authenticator de forma segura usa: <#C084FC>/2fa disable <código_6_dígitos></#C084FC></#F5D0FE>"));
            return true;
        }

        if (target.equals("all") || target.equals("todo")) {
            boolean unlinkedAny = false;
            if (profile.getDiscordId() != null && !profile.getDiscordId().isEmpty()) {
                plugin.getDiscordManager().unlinkDiscord(player);
                unlinkedAny = true;
            }
            if (profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                profile.setEmail(null);
                unlinkedAny = true;
            }
            if (profile.is2FAEnabled()) {
                profile.set2FAEnabled(false);
                profile.setTotpSecret(null);
                profile.setBackupCodes(null);
                unlinkedAny = true;
            }
            plugin.getDatabaseManager().saveProfile(profile);

            if (unlinkedAny) {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Security</bold></gradient> <dark_gray>»</dark_gray> <yellow>✔ Se han desvinculado todos los métodos 2FA de tu cuenta.</yellow>"));
            } else {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Security</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>No tenías ningún método 2FA activo.</#F5D0FE>"));
            }
            return true;
        }

        player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /unlink <discord|email|all></#F5D0FE>"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("discord", "email", "2fa", "all");
            return options.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}
