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

public class RegisterCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public RegisterCommand(SmartLogin plugin) {
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

        PlayerProfile existing = plugin.getAuthManager().getCachedProfile(player.getUniqueId());
        if (existing != null && existing.getPasswordHash() != null) {
            player.sendMessage(plugin.getLocaleManager().getMessage("login_prompt"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getLocaleManager().getMessage("register_prompt"));
            return true;
        }

        String password = args[0];
        String confirm = args[1];

        if (!password.equals(confirm)) {
            player.sendMessage(plugin.getLocaleManager().getMessage("passwords_dont_match"));
            return true;
        }

        int minLength = plugin.getConfigManager().getMinPasswordLength();
        if (password.length() < minLength) {
            player.sendMessage(plugin.getLocaleManager().getMessage("password_too_short", "min", String.valueOf(minLength)));
            return true;
        }

        if (plugin.getConfigManager().getBlockedPasswords().contains(password.toLowerCase())) {
            player.sendMessage(plugin.getLocaleManager().getMessage("password_blocked"));
            return true;
        }

        String ip = plugin.getSessionShield().getPlayerIp(player);
        int maxPerIp = plugin.getConfigManager().getMaxAccountsPerIp();

        plugin.getDatabaseManager().countAccountsByIp(ip).thenAccept(count -> {
            if (maxPerIp > 0 && count >= maxPerIp) {
                player.sendMessage(plugin.getLocaleManager().getMessage("max_accounts_reached", "max", String.valueOf(maxPerIp)));
                return;
            }

            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hashPassword(password, salt);

            PlayerProfile profile = new PlayerProfile(
                    player.getUniqueId(),
                    player.getName(),
                    hash,
                    salt,
                    false,
                    null,
                    ip,
                    System.currentTimeMillis(),
                    plugin.getAutoLoginDetector().isJavaPremium(player),
                    plugin.getAutoLoginDetector().isBedrock(player)
            );

            plugin.getDatabaseManager().saveProfile(profile).thenRun(() -> {
                plugin.getAuthManager().cacheProfile(profile);
                plugin.getAuthManager().setState(player.getUniqueId(), AuthManager.AuthState.LOGGED_IN);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                    player.sendMessage(plugin.getLocaleManager().getMessage("register_success"));
                });
            });
        });

        return true;
    }
}
