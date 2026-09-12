package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.PasswordHasher;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RegisterCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public RegisterCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Este comando es solo para jugadores.</#F5D0FE>"));
            return true;
        }

        if (plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-already-logged-in", player));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getLocaleManager().getComponent("prompt-register", player));
            return true;
        }

        String pass1 = args[0];
        String pass2 = args.length >= 2 ? args[1] : pass1;

        if (!pass1.equals(pass2)) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-passwords-mismatch", player));
            return true;
        }

        int minLen = plugin.getModularConfig().getConfig().getInt("password-policy.min-length", 4);
        int maxLen = plugin.getModularConfig().getConfig().getInt("password-policy.max-length", 32);

        if (pass1.length() < minLen || pass1.length() > maxLen) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-password-length", player));
            return true;
        }

        var disallowed = plugin.getModularConfig().getConfig().getStringList("password-policy.disallowed-passwords");
        if (disallowed != null && (disallowed.contains(pass1.toLowerCase()) || pass1.equalsIgnoreCase(player.getName()))) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-password-too-weak", player));
            return true;
        }

        plugin.getDatabaseManager().loadProfile(player.getUniqueId()).thenCompose(p -> {
            if (p != null) return java.util.concurrent.CompletableFuture.completedFuture(p);
            return plugin.getDatabaseManager().loadProfileByName(player.getName());
        }).thenAccept(existing -> {
            if (existing != null && existing.getPasswordHash() != null && !existing.getPasswordHash().trim().isEmpty()) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-already-registered", player));
                return;
            }

            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hash(pass1, salt);

            PlayerProfile newProfile = new PlayerProfile(
                    player.getUniqueId(),
                    player.getName(),
                    hash,
                    salt,
                    false,
                    null,
                    null,
                    player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "127.0.0.1",
                    System.currentTimeMillis(),
                    false,
                    plugin.getAutoLoginDetector().isBedrockPlayer(player)
            );

            plugin.getDatabaseManager().saveProfile(newProfile).thenRun(() -> {
                plugin.getAuthManager().cacheProfile(player.getUniqueId(), newProfile);
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getAuthManager().completeAuthentication(player, "success-registered");
                });
            });
        });

        return true;
    }
}
