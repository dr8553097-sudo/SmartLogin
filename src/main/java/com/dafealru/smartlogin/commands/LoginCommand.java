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
            player.sendMessage(plugin.getLocaleManager().getMessage("already_logged_in"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getLocaleManager().getMessage("login_prompt"));
            return true;
        }

        PlayerProfile profile = plugin.getAuthManager().getCachedProfile(player.getUniqueId());
        if (profile == null || profile.getPasswordHash() == null) {
            player.sendMessage(plugin.getLocaleManager().getMessage("register_prompt"));
            return true;
        }

        String inputPassword = args[0];
        boolean valid = PasswordHasher.verifyPassword(inputPassword, profile.getSalt(), profile.getPasswordHash());

        if (valid) {
            if (profile.is2FAEnabled()) {
                plugin.getAuthManager().setState(player.getUniqueId(), AuthManager.AuthState.AWAITING_2FA);
                player.sendMessage(plugin.getLocaleManager().getMessage("two_factor_prompt"));
                return true;
            }

            plugin.getAuthManager().setState(player.getUniqueId(), AuthManager.AuthState.LOGGED_IN);
            plugin.getAuthManager().resetAttempts(player.getUniqueId());
            player.removePotionEffect(PotionEffectType.BLINDNESS);

            profile.setLastIp(plugin.getSessionShield().getPlayerIp(player));
            profile.setLastLoginTimestamp(System.currentTimeMillis());
            plugin.getDatabaseManager().saveProfile(profile);

            player.sendMessage(plugin.getLocaleManager().getMessage("login_success", "player", player.getName()));
        } else {
            int attempts = plugin.getAuthManager().incrementAttempts(player.getUniqueId());
            int remaining = Math.max(0, 5 - attempts);
            player.sendMessage(plugin.getLocaleManager().getMessage("wrong_password", "attempts", String.valueOf(remaining)));

            if (attempts >= 5) {
                player.kick(plugin.getLocaleManager().parse("<red>Maximum login attempts exceeded!</red>"));
            }
        }
        return true;
    }
}
