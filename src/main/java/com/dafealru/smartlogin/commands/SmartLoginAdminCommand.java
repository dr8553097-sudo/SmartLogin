package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SmartLoginAdminCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public SmartLoginAdminCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("smartlogin.admin")) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("no_permission"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("setup") || args[0].equalsIgnoreCase("wizard")) {
            plugin.getSetupWizardManager().sendWizard(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            plugin.getConfigManager().load();
            plugin.getLocaleManager().load();
            sender.sendMessage(plugin.getLocaleManager().getMessage("admin_reload"));
            return true;
        }

        if (sub.equals("pinpad") && sender instanceof Player player) {
            plugin.getPinPadGUI().openPinPad(player);
            return true;
        }

        if (sub.equals("unregister") && args.length > 1) {
            String targetName = args[1];
            plugin.getDatabaseManager().loadProfileByName(targetName).thenAccept(profile -> {
                if (profile == null) {
                    sender.sendMessage(plugin.getLocaleManager().getMessage("player_not_found"));
                } else {
                    plugin.getDatabaseManager().deleteProfile(profile.getUuid()).thenRun(() -> {
                        sender.sendMessage(plugin.getLocaleManager().getMessage("admin_unregistered", "player", targetName));
                    });
                }
            });
            return true;
        }

        if (sub.equals("reset2fa") && args.length > 1) {
            String targetName = args[1];
            plugin.getDatabaseManager().loadProfileByName(targetName).thenAccept(profile -> {
                if (profile == null) {
                    sender.sendMessage(plugin.getLocaleManager().getMessage("player_not_found"));
                } else {
                    profile.set2FAEnabled(false);
                    profile.setTotpSecret(null);
                    plugin.getDatabaseManager().saveProfile(profile).thenRun(() -> {
                        sender.sendMessage(plugin.getLocaleManager().getMessage("admin_reset_2fa", "player", targetName));
                    });
                }
            });
            return true;
        }

        if (sub.equals("toggle") && args.length > 1) {
            String feature = args[1].toLowerCase();
            plugin.getConfigManager().toggleOption(feature);
            sender.sendMessage(plugin.getLocaleManager().parse("<green>✔ Option <white>" + feature + "</white> toggled!</green>"));
            plugin.getSetupWizardManager().sendWizard(sender);
            return true;
        }

        sender.sendMessage(plugin.getLocaleManager().parse("<yellow>Usage: /smartlogin [setup|reload|unregister <player>|reset2fa <player>|pinpad]</yellow>"));
        return true;
    }
}
