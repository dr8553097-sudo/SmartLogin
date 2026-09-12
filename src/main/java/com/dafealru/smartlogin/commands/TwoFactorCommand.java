package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.auth.AuthManager;
import com.dafealru.smartlogin.crypto.TotpEngine;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class TwoFactorCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public TwoFactorCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only for players.");
            return true;
        }

        PlayerProfile profile = plugin.getAuthManager().getCachedProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(plugin.getLocaleManager().getMessage("register_prompt"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9d4edd:#00f0ff><bold>SmartLogin 2FA Command</bold></gradient>"));
            player.sendMessage(plugin.getLocaleManager().parse("<yellow>/2fa setup</yellow> <gray>- Generate QR Code Map for Google Auth</gray>"));
            player.sendMessage(plugin.getLocaleManager().parse("<yellow>/2fa verify <code></yellow> <gray>- Confirm and unlock 2FA</gray>"));
            player.sendMessage(plugin.getLocaleManager().parse("<yellow>/2fa disable <code></yellow> <gray>- Disable 2FA on account</gray>"));
            return true;
        }

        String sub = args[0].toLowerCase();

        // 1. SETUP
        if (sub.equals("setup")) {
            if (profile.is2FAEnabled()) {
                player.sendMessage(plugin.getLocaleManager().getMessage("two_factor_already_enabled"));
                return true;
            }

            String secret = plugin.getQrMapManager().start2FASetup(player);
            player.sendMessage(plugin.getLocaleManager().getMessage("two_factor_map_given"));
            player.sendMessage(plugin.getLocaleManager().getMessage("two_factor_secret_chat", "secret", secret));
            return true;
        }

        // 2. VERIFY
        if (sub.equals("verify") || sub.equals("code") || sub.matches("\\d{6}")) {
            String code = sub.equals("verify") && args.length > 1 ? args[1] : (sub.matches("\\d{6}") ? sub : (args.length > 1 ? args[1] : ""));

            // Check if completing initial setup
            String pendingSecret = plugin.getQrMapManager().getPendingSecret(player.getUniqueId());
            if (pendingSecret != null) {
                if (TotpEngine.verifyCode(pendingSecret, code)) {
                    profile.set2FAEnabled(true);
                    profile.setTotpSecret(pendingSecret);
                    plugin.getDatabaseManager().saveProfile(profile);
                    plugin.getQrMapManager().removePending(player.getUniqueId());

                    player.sendMessage(plugin.getLocaleManager().getMessage("two_factor_success"));
                    return true;
                } else {
                    player.sendMessage(plugin.getLocaleManager().getMessage("two_factor_invalid"));
                    return true;
                }
            }

            // Check login verification
            if (profile.is2FAEnabled()) {
                if (TotpEngine.verifyCode(profile.getTotpSecret(), code)) {
                    plugin.getAuthManager().setState(player.getUniqueId(), AuthManager.AuthState.LOGGED_IN);
                    player.removePotionEffect(PotionEffectType.BLINDNESS);

                    profile.setLastIp(plugin.getSessionShield().getPlayerIp(player));
                    profile.setLastLoginTimestamp(System.currentTimeMillis());
                    plugin.getDatabaseManager().saveProfile(profile);

                    player.sendMessage(plugin.getLocaleManager().getMessage("two_factor_success"));
                } else {
                    player.sendMessage(plugin.getLocaleManager().getMessage("two_factor_invalid"));
                }
            }
            return true;
        }

        // 3. DISABLE
        if (sub.equals("disable")) {
            if (!profile.is2FAEnabled()) {
                player.sendMessage(plugin.getLocaleManager().getMessage("two_factor_not_enabled"));
                return true;
            }
            if (args.length < 2 || !TotpEngine.verifyCode(profile.getTotpSecret(), args[1])) {
                player.sendMessage(plugin.getLocaleManager().getMessage("two_factor_invalid"));
                return true;
            }

            profile.set2FAEnabled(false);
            profile.setTotpSecret(null);
            plugin.getDatabaseManager().saveProfile(profile);
            player.sendMessage(plugin.getLocaleManager().getMessage("two_factor_disabled"));
            return true;
        }

        return true;
    }
}
