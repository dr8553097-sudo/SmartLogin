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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can change their password.");
            return true;
        }

        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-not-logged-in", player));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getLocaleManager().getComponent("prompt-change-password", player));
            return true;
        }

        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
        if (profile == null) return true;

        String oldPass = args[0];
        String newPass = args[1];

        if (!PasswordHasher.verify(oldPass, profile.getSalt(), profile.getPasswordHash())) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-wrong-old-password", player));
            return true;
        }

        int minLen = plugin.getModularConfig().getConfig().getInt("password-policy.min-length", 6);
        int maxLen = plugin.getModularConfig().getConfig().getInt("password-policy.max-length", 32);
        if (newPass.length() < minLen || newPass.length() > maxLen) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-password-length", player));
            return true;
        }

        String newSalt = PasswordHasher.generateSalt();
        String newHash = PasswordHasher.hash(newPass, newSalt);

        profile.setSalt(newSalt);
        profile.setPasswordHash(newHash);
        plugin.getDatabaseManager().saveProfile(profile);

        player.sendMessage(plugin.getLocaleManager().getComponent("success-password-changed", player));
        return true;
    }
}
