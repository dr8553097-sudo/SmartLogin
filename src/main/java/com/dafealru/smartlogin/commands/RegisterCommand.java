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
            sender.sendMessage("This command is only for players.");
            return true;
        }

        if (plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-already-logged-in", player));
            return true;
        }

        PlayerProfile existing = plugin.getAuthManager().getProfile(player.getUniqueId());
        if (existing != null && existing.getPasswordHash() != null) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-already-registered", player));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getLocaleManager().getComponent("prompt-register", player));
            return true;
        }

        String pass1 = args[0];
        String pass2 = args[1];

        if (!pass1.equals(pass2)) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-passwords-mismatch", player));
            return true;
        }

        int minLen = plugin.getModularConfig().getConfig().getInt("password-policy.min-length", 6);
        int maxLen = plugin.getModularConfig().getConfig().getInt("password-policy.max-length", 32);

        if (pass1.length() < minLen || pass1.length() > maxLen) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-password-length", player));
            return true;
        }

        var disallowed = plugin.getModularConfig().getConfig().getStringList("password-policy.disallowed-passwords");
        if (disallowed.contains(pass1.toLowerCase()) || pass1.equalsIgnoreCase(player.getName())) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-password-too-weak", player));
            return true;
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
                player.getAddress().getAddress().getHostAddress(),
                System.currentTimeMillis(),
                false,
                plugin.getAutoLoginDetector().isBedrockPlayer(player)
        );

        plugin.getDatabaseManager().saveProfile(newProfile).thenRun(() -> {
            plugin.getAuthManager().cacheProfile(player.getUniqueId(), newProfile);
            plugin.getAuthManager().setAuthenticated(player.getUniqueId(), true);

            plugin.getAuthHudManager().stopHud(player);
            plugin.getAuthHudManager().playSuccessSound(player);
            plugin.getProxyBridge().sendToLobby(player);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }

            plugin.getSpawnManager().handleLoginRestore(player);
            player.sendMessage(plugin.getLocaleManager().getComponent("success-registered", player));
        });

        return true;
    }
}
