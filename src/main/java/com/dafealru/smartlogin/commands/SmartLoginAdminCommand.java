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
            if (sender instanceof Player player) {
                plugin.getAdminPanelGUI().openPanel(player);
            } else {
                plugin.getSetupWizardManager().sendSetupForm(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "gui":
            case "panel":
            case "menu":
                if (sender instanceof Player player) {
                    plugin.getAdminPanelGUI().openPanel(player);
                } else {
                    sender.sendMessage("GUI panel is only accessible in-game.");
                }
                break;
            case "setup":
                plugin.getSetupWizardManager().sendSetupForm(sender);
                break;
            case "wizard":
                if (args.length >= 3) {
                    try {
                        int step = Integer.parseInt(args[1]);
                        String choice = args[2];
                        plugin.getSetupWizardManager().handleStepChoice(sender, step, choice);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Component.text("Invalid step number.", NamedTextColor.RED));
                    }
                } else if (args.length >= 2) {
                    try {
                        int step = Integer.parseInt(args[1]);
                        plugin.getSetupWizardManager().sendStep(sender, step);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Component.text("Invalid step number.", NamedTextColor.RED));
                    }
                } else {
                    plugin.getSetupWizardManager().sendSetupForm(sender);
                }
                break;
            case "resetsetup":
                plugin.getModularConfig().getConfig().set("setup-completed", false);
                plugin.getModularConfig().saveConfig();
                sender.sendMessage(Component.text("✔ Setup status reset! The wizard will launch on next admin join.", NamedTextColor.GREEN));
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
            case "unregister":
            case "unreg":
                if (args.length > 1) {
                    String targetName = args[1];
                    plugin.getDatabaseManager().loadProfileByName(targetName).thenAccept(p -> {
                        if (p != null) {
                            plugin.getDatabaseManager().deleteProfile(p.getUuid()).thenRun(() -> {
                                plugin.getAuthManager().uncache(p.getUuid());
                                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green>✔ Cuenta de <yellow>" + targetName + "</yellow> eliminada correctamente.</green>"));
                            });
                        } else {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>✖ No se encontró al jugador <yellow>" + targetName + "</yellow> en la base de datos.</red>"));
                        }
                    });
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>Uso: /smartlogin unregister <usuario></red>"));
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
                if (sender instanceof Player player) {
                    plugin.getAdminPanelGUI().openPanel(player);
                } else {
                    plugin.getSetupWizardManager().sendSetupForm(sender);
                }
                break;
        }

        return true;
    }
}
