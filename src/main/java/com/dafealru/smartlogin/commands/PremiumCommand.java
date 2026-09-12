package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PremiumCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public PremiumCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can toggle premium status.");
            return true;
        }

        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-not-logged-in", player));
            return true;
        }

        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
        if (profile == null) return true;

        boolean nowPrem = !profile.isPremium();
        profile.setPremium(nowPrem);
        plugin.getDatabaseManager().saveProfile(profile);

        if (nowPrem) {
            player.sendMessage(plugin.getLocaleManager().getComponent("success-premium-enabled", player));
        } else {
            player.sendMessage(plugin.getLocaleManager().getComponent("success-premium-disabled", player));
        }
        return true;
    }
}
