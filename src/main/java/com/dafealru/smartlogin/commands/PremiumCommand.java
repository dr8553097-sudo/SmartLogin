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
        if (!(sender instanceof Player player)) return true;

        PlayerProfile profile = plugin.getAuthManager().getCachedProfile(player.getUniqueId());
        if (profile == null) return true;

        boolean toggleTo = cmd.getName().equalsIgnoreCase("premium");
        profile.setPremium(toggleTo);
        plugin.getDatabaseManager().saveProfile(profile);

        if (toggleTo) {
            player.sendMessage(plugin.getLocaleManager().parse("<green>✔ Mojang Premium Auto-Login has been enabled for your account!</green>"));
        } else {
            player.sendMessage(plugin.getLocaleManager().parse("<yellow>✔ Switched back to offline/password mode.</yellow>"));
        }
        return true;
    }
}
