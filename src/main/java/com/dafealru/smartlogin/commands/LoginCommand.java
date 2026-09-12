package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.auth.AuthManager;
import com.dafealru.smartlogin.crypto.PasswordHasher;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class LoginCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public LoginCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        if (plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-already-logged-in", player));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getLocaleManager().getComponent("prompt-login", player));
            return true;
        }

        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
        if (profile == null || profile.getPasswordHash() == null) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-not-registered", player));
            return true;
        }

        String inputPassword = args[0];
        boolean valid = PasswordHasher.verify(inputPassword, profile.getSalt(), profile.getPasswordHash());

        if (valid) {
            if (profile.is2FAEnabled()) {
                player.sendMessage(plugin.getLocaleManager().getComponent("prompt-2fa-verify", player));
                return true;
            }

            plugin.getAuthManager().setAuthenticated(player.getUniqueId(), true);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);

            profile.setLastIp(player.getAddress().getAddress().getHostAddress());
            profile.setLastLoginTimestamp(System.currentTimeMillis());
            plugin.getDatabaseManager().saveProfile(profile);

            plugin.getSpawnManager().handleLoginRestore(player);
            player.sendMessage(plugin.getLocaleManager().getComponent("success-logged-in", player));
        } else {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-wrong-password", player));
        }
        return true;
    }
}
