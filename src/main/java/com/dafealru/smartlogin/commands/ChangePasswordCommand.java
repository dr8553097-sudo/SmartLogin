package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.PasswordHasher;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChangePasswordCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public ChangePasswordCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            player.sendMessage(plugin.getLocaleManager().getMessage("login_prompt"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getLocaleManager().parse("<yellow>Usage: <gold>/changepassword <oldPassword> <newPassword></gold></yellow>"));
            return true;
        }

        PlayerProfile profile = plugin.getAuthManager().getCachedProfile(player.getUniqueId());
        if (profile == null) return true;

        String oldPass = args[0];
        String newPass = args[1];

        if (!PasswordHasher.verifyPassword(oldPass, profile.getSalt(), profile.getPasswordHash())) {
            player.sendMessage(plugin.getLocaleManager().getMessage("wrong_password", "attempts", "1"));
            return true;
        }

        if (newPass.length() < plugin.getConfigManager().getMinPasswordLength()) {
            player.sendMessage(plugin.getLocaleManager().getMessage("password_too_short", "min", String.valueOf(plugin.getConfigManager().getMinPasswordLength())));
            return true;
        }

        String newSalt = PasswordHasher.generateSalt();
        String newHash = PasswordHasher.hashPassword(newPass, newSalt);

        profile.setSalt(newSalt);
        profile.setPasswordHash(newHash);
        plugin.getDatabaseManager().saveProfile(profile);

        player.sendMessage(plugin.getLocaleManager().getMessage("password_changed"));
        return true;
    }
}
