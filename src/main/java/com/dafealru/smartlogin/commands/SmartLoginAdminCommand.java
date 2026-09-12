package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class SmartLoginAdminCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public SmartLoginAdminCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("smartlogin.admin")) {
            sender.sendMessage(Component.text("You lack permission smartlogin.admin", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            plugin.getSetupWizardManager().sendSetupForm(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "setup":
                plugin.getSetupWizardManager().sendSetupForm(sender);
                break;
            case "toggle":
                if (args.length > 1) {
                    plugin.getSetupWizardManager().handleToggle(sender, args[1]);
                }
                break;
            case "finishsetup":
                plugin.getSetupWizardManager().finishSetup(sender);
                break;
            case "setspawn":
                if (sender instanceof Player player) {
                    plugin.getSpawnManager().setAuthSpawn(player.getLocation());
                    player.sendMessage(Component.text("✔ SmartLogin auth spawn location set to your current position!", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage("Only players can set spawn.");
                }
                break;
            case "history":
            case "audit":
                if (args.length > 1) {
                    plugin.getAuditManager().showPlayerHistory(sender, args[1]);
                } else {
                    sender.sendMessage(Component.text("Usage: /smartlogin history <username>", NamedTextColor.RED));
                }
                break;
            case "setpassword":
            case "changepass":
                if (args.length > 2) {
                    String target = args[1];
                    String newPass = args[2];
                    plugin.getAuditManager().setPlayerPassword(target, newPass).thenAccept(ok -> {
                        if (ok) {
                            sender.sendMessage(Component.text("✔ Password for " + target + " updated successfully!", NamedTextColor.GREEN));
                        } else {
                            sender.sendMessage(Component.text("✖ Player not found in database.", NamedTextColor.RED));
                        }
                    });
                } else {
                    sender.sendMessage(Component.text("Usage: /smartlogin setpassword <username> <new_password>", NamedTextColor.RED));
                }
                break;
            case "backup":
                sender.sendMessage(Component.text("Creating database backup snapshot...", NamedTextColor.YELLOW));
                File backupFile = plugin.getAuditManager().createDatabaseBackup();
                if (backupFile != null) {
                    sender.sendMessage(Component.text("✔ Backup created: " + backupFile.getName(), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("✖ Failed to create database backup.", NamedTextColor.RED));
                }
                break;
            case "reload":
                plugin.getModularConfig().loadAll();
                plugin.getLocaleManager().loadLanguages();
                sender.sendMessage(Component.text("✔ SmartLogin modular configs (including 2fa/) and languages reloaded!", NamedTextColor.GREEN));
                break;
            case "import":
                if (args.length > 1 && args[1].equalsIgnoreCase("authme")) {
                    sender.sendMessage(Component.text("Starting AuthMe database import...", NamedTextColor.YELLOW));
                    plugin.getMigrationManager().importAuthMe(sender).thenAccept(count -> {
                        sender.sendMessage(Component.text("✔ Successfully imported " + count + " accounts from AuthMe!", NamedTextColor.GREEN));
                    });
                } else if (args.length > 1 && args[1].equalsIgnoreCase("nlogin")) {
                    sender.sendMessage(Component.text("Starting nLogin database import...", NamedTextColor.YELLOW));
                    plugin.getMigrationManager().importNLogin(sender).thenAccept(count -> {
                        sender.sendMessage(Component.text("✔ Successfully imported " + count + " accounts from nLogin!", NamedTextColor.GREEN));
                    });
                } else {
                    sender.sendMessage(Component.text("Usage: /smartlogin import <authme|nlogin>", NamedTextColor.RED));
                }
                break;
            default:
                plugin.getSetupWizardManager().sendSetupForm(sender);
                break;
        }

        return true;
    }
}
