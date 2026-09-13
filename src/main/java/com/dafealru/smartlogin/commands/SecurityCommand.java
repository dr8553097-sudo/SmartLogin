package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SecurityCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public SecurityCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLocaleManager().getComponent("error-only-players", null));
            return true;
        }

        plugin.getSecurityCenterGUI().openGUI(player);
        return true;
    }
}
