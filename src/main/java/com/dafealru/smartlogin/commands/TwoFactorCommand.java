package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.TotpEngine;
import com.dafealru.smartlogin.database.PlayerProfile;
import com.dafealru.smartlogin.twofactor.BackupCodeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TwoFactorCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public TwoFactorCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be executed by players.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("════════════ 📱 SmartLogin 2FA ════════════", NamedTextColor.GOLD));
            player.sendMessage(Component.text("/2fa setup ", NamedTextColor.YELLOW).append(Component.text("- Receive in-game QR code map & recovery codes", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/2fa verify <code> ", NamedTextColor.YELLOW).append(Component.text("- Verify 6-digit Google Authenticator code", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/2fa recovery <code> ", NamedTextColor.YELLOW).append(Component.text("- Use single-use emergency backup code", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/2fa disable <code> ", NamedTextColor.YELLOW).append(Component.text("- Disable 2FA protection", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("═══════════════════════════════════════════", NamedTextColor.GOLD));
            return true;
        }

        String sub = args[0].toLowerCase();
        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());

        if (sub.equals("setup")) {
            if (profile == null || profile.getPasswordHash() == null) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-not-registered", player));
                return true;
            }

            List<String> recoveryCodes = plugin.getTwoFactorManager().setupNew2FA(profile);
            plugin.getQrMapManager().giveQrMap(player, profile.getTotpSecret());

            player.sendMessage(Component.text("═══════════ 🔐 2FA RECOVERY CODES ═══════════", NamedTextColor.DARK_RED, TextDecoration.BOLD));
            player.sendMessage(Component.text("SAVE THESE CODES! If you lose your phone, use /2fa recovery <code>:", NamedTextColor.GOLD));
            for (String code : recoveryCodes) {
                player.sendMessage(Component.text("  ➤  ", NamedTextColor.YELLOW).append(Component.text(code, NamedTextColor.GREEN, TextDecoration.BOLD)));
            }
            player.sendMessage(Component.text("═══════════════════════════════════════════════", NamedTextColor.DARK_RED));
            return true;
        }

        if (sub.equals("verify")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Usage: /2fa verify <6-digit-code>", NamedTextColor.RED));
                return true;
            }
            if (profile == null || profile.getTotpSecret() == null) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-not-setup", player));
                return true;
            }

            try {
                int code = Integer.parseInt(args[1]);
                if (TotpEngine.verifyCode(profile.getTotpSecret(), code, 1)) {
                    profile.set2FAEnabled(true);
                    plugin.getDatabaseManager().saveProfile(profile);
                    plugin.getAuthManager().setAuthenticated(player.getUniqueId(), true);
                    plugin.getAuthHudManager().stopHud(player);
                    plugin.getAuthHudManager().playSuccessSound(player);
                    plugin.getProxyBridge().sendToLobby(player);
                    player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                    player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
                    if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                    }
                    plugin.getSpawnManager().handleLoginRestore(player);
                    player.sendMessage(plugin.getLocaleManager().getComponent("success-2fa-enabled", player));
                } else {
                    player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-invalid", player));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-invalid", player));
            }
            return true;
        }

        if (sub.equals("recovery")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Usage: /2fa recovery <8-digit-code>", NamedTextColor.RED));
                return true;
            }
            if (profile == null || !profile.is2FAEnabled()) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-not-setup", player));
                return true;
            }

            if (BackupCodeManager.verifyAndConsume(profile, args[1])) {
                plugin.getDatabaseManager().saveProfile(profile);
                plugin.getAuthManager().setAuthenticated(player.getUniqueId(), true);
                plugin.getAuthHudManager().stopHud(player);
                plugin.getAuthHudManager().playSuccessSound(player);
                plugin.getProxyBridge().sendToLobby(player);
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
                plugin.getSpawnManager().handleLoginRestore(player);
                player.sendMessage(Component.text("✔ Recovery code accepted! Emergency login successful.", NamedTextColor.GREEN, TextDecoration.BOLD));
            } else {
                player.sendMessage(Component.text("✖ Invalid or already consumed recovery code.", NamedTextColor.RED));
            }
            return true;
        }

        if (sub.equals("disable")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Usage: /2fa disable <6-digit-code>", NamedTextColor.RED));
                return true;
            }
            if (profile == null || !profile.is2FAEnabled()) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-not-setup", player));
                return true;
            }

            try {
                int code = Integer.parseInt(args[1]);
                if (TotpEngine.verifyCode(profile.getTotpSecret(), code, 1)) {
                    profile.set2FAEnabled(false);
                    profile.setTotpSecret(null);
                    profile.setBackupCodes(null);
                    plugin.getDatabaseManager().saveProfile(profile);
                    player.sendMessage(plugin.getLocaleManager().getComponent("success-2fa-disabled", player));
                } else {
                    player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-invalid", player));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-2fa-invalid", player));
            }
            return true;
        }

        return true;
    }
}
