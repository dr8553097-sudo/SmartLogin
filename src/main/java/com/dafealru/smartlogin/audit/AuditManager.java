package com.dafealru.smartlogin.audit;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.PasswordHasher;
import com.dafealru.smartlogin.database.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class AuditManager {

    private final SmartLogin plugin;

    public AuditManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public void showPlayerHistory(CommandSender sender, String username) {
        sender.sendMessage(Component.text("🔍 Looking up profile for " + username + "...", NamedTextColor.YELLOW));
        plugin.getDatabaseManager().loadProfileByName(username).thenAccept(profile -> {
            if (profile == null) {
                sender.sendMessage(Component.text("✖ Player profile not found.", NamedTextColor.RED));
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String lastLoginDate = profile.getLastLoginTimestamp() > 0 ? sdf.format(new Date(profile.getLastLoginTimestamp())) : "Never";

            sender.sendMessage(Component.text("════════════ 🛡️ SMARTLOGIN AUDIT ════════════", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("• Username: ", NamedTextColor.WHITE).append(Component.text(profile.getUsername(), NamedTextColor.YELLOW, TextDecoration.BOLD)));
            sender.sendMessage(Component.text("• UUID: ", NamedTextColor.WHITE).append(Component.text(profile.getUuid().toString(), NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("• Last IP: ", NamedTextColor.WHITE).append(Component.text(profile.getLastIp() != null ? profile.getLastIp() : "N/A", NamedTextColor.AQUA)));
            sender.sendMessage(Component.text("• Last Login: ", NamedTextColor.WHITE).append(Component.text(lastLoginDate, NamedTextColor.GREEN)));
            sender.sendMessage(Component.text("• 2FA TOTP: ", NamedTextColor.WHITE).append(Component.text(profile.is2FAEnabled() ? "✔ ENABLED" : "✖ DISABLED", profile.is2FAEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)));
            sender.sendMessage(Component.text("• Discord Linked: ", NamedTextColor.WHITE).append(Component.text(profile.getDiscordId() != null ? "✔ (" + profile.getDiscordId() + ")" : "✖ NONE", profile.getDiscordId() != null ? NamedTextColor.GREEN : NamedTextColor.RED)));
            sender.sendMessage(Component.text("• Mojang Premium: ", NamedTextColor.WHITE).append(Component.text(profile.isPremium() ? "✔ YES" : "✖ NO", profile.isPremium() ? NamedTextColor.GREEN : NamedTextColor.RED)));
            sender.sendMessage(Component.text("• Bedrock Floodgate: ", NamedTextColor.WHITE).append(Component.text(profile.isBedrock() ? "✔ YES" : "✖ NO", profile.isBedrock() ? NamedTextColor.GREEN : NamedTextColor.RED)));
            sender.sendMessage(Component.text("═════════════════════════════════════════════", NamedTextColor.GOLD));
        });
    }

    public CompletableFuture<Boolean> setPlayerPassword(String username, String newPassword) {
        return plugin.getDatabaseManager().loadProfileByName(username).thenApply(profile -> {
            if (profile == null) return false;
            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hash(newPassword, salt);
            profile.setSalt(salt);
            profile.setPasswordHash(hash);
            profile.setLastLoginTimestamp(0);
            plugin.getDatabaseManager().saveProfile(profile);
            plugin.getAuthManager().cacheProfile(profile.getUuid(), profile);

            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = org.bukkit.Bukkit.getPlayerExact(username);
                if (online != null && online.isOnline()) {
                    plugin.getAuthManager().removeAuthenticated(online.getUniqueId());
                    online.kick(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient>\n\n<#F5D0FE>Tu contraseña fue modificada por un administrador.\nPor favor vuelve a ingresar con tu nueva clave.</#F5D0FE>"));
                }
            });

            return true;
        });
    }

    public File createDatabaseBackup() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "smartlogin.db");
            if (!dbFile.exists()) return null;

            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) backupDir.mkdirs();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String filename = "smartlogin_backup_" + sdf.format(new Date()) + ".db";
            File dest = new File(backupDir, filename);

            try (FileChannel src = new FileInputStream(dbFile).getChannel();
                 FileChannel dst = new FileOutputStream(dest).getChannel()) {
                dst.transferFrom(src, 0, src.size());
            }
            return dest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
