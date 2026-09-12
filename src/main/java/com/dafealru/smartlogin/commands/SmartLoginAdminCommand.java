package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SmartLoginAdminCommand implements CommandExecutor, TabCompleter {

    private final SmartLogin plugin;

    public SmartLoginAdminCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("smartlogin.admin")) {
            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>No tienes permisos suficientes (smartlogin.admin).</red>"));
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getAdminPanelGUI().openPanel(player);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help":
            case "?":
                sendHelp(sender);
                break;
            case "gui":
            case "panel":
            case "menu":
                if (sender instanceof Player player) {
                    plugin.getAdminPanelGUI().openPanel(player);
                } else {
                    sender.sendMessage("El panel GUI solo está disponible para jugadores dentro del juego.");
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
                        sender.sendMessage(Component.text("Paso inválido.", NamedTextColor.RED));
                    }
                } else if (args.length >= 2) {
                    try {
                        int step = Integer.parseInt(args[1]);
                        plugin.getSetupWizardManager().sendStep(sender, step);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Component.text("Paso inválido.", NamedTextColor.RED));
                    }
                } else {
                    plugin.getSetupWizardManager().sendSetupForm(sender);
                }
                break;
            case "resetsetup":
                plugin.getModularConfig().getConfig().set("setup-completed", false);
                plugin.getModularConfig().saveConfig();
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green>✔ Estado del setup reiniciado. El asistente se iniciará en el próximo ingreso de un admin.</green>"));
                break;
            case "finishsetup":
                plugin.getSetupWizardManager().finishSetup(sender);
                break;
            case "setspawn":
                if (sender instanceof Player player) {
                    plugin.getSpawnManager().setAuthSpawn(player.getLocation());
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green>✔ Ubicación del spawn de autenticación establecida en tu posición.</green>"));
                } else {
                    sender.sendMessage("Solo los jugadores pueden ejecutar /smartlogin setspawn.");
                }
                break;
            case "history":
            case "audit":
                if (args.length > 1) {
                    plugin.getAuditManager().showPlayerHistory(sender, args[1]);
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>Uso: /smartlogin history <usuario></red>"));
                }
                break;
            case "setpassword":
            case "changepass":
                if (args.length > 2) {
                    String target = args[1];
                    String newPass = args[2];
                    plugin.getAuditManager().setPlayerPassword(target, newPass).thenAccept(ok -> {
                        if (ok) {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green>✔ Contraseña de <yellow>" + target + "</yellow> actualizada correctamente.</green>"));
                        } else {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>✖ No se encontró al jugador en la base de datos.</red>"));
                        }
                    });
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>Uso: /smartlogin setpassword <usuario> <nueva_clave></red>"));
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
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <yellow>Creando copia de seguridad de la base de datos...</yellow>"));
                File backupFile = plugin.getAuditManager().createDatabaseBackup();
                if (backupFile != null) {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green>✔ Copia de seguridad creada: <yellow>" + backupFile.getName() + "</yellow></green>"));
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>✖ Error creando la copia de seguridad.</red>"));
                }
                break;
            case "reload":
                plugin.getModularConfig().loadAll();
                plugin.getLocaleManager().loadLanguages();
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green>✔ Configuraciones modulares, suite 2FA e idiomas recargados correctamente.</green>"));
                break;
            case "migrate":
            case "import":
                if (args.length > 1) {
                    String source = args[1].toLowerCase();
                    if (source.equals("authme")) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <yellow>Iniciando migración desde AuthMe...</yellow>"));
                        plugin.getMigrationManager().importAuthMe(sender).thenAccept(count -> {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green>✔ ¡Se importaron exitosamente <bold>" + count + "</bold> cuentas desde AuthMe!</green>"));
                        });
                    } else if (source.equals("nlogin")) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <yellow>Iniciando migración desde nLogin...</yellow>"));
                        plugin.getMigrationManager().importNLogin(sender).thenAccept(count -> {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green>✔ ¡Se importaron exitosamente <bold>" + count + "</bold> cuentas desde nLogin!</green>"));
                        });
                    } else if (source.equals("fastlogin")) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <yellow>Iniciando migración de estados Premium desde FastLogin...</yellow>"));
                        plugin.getMigrationManager().importFastLogin(sender).thenAccept(count -> {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green>✔ ¡Se sincronizaron <bold>" + count + "</bold> cuentas desde FastLogin!</green>"));
                        });
                    } else {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>Fuente desconocida. Usa: /smartlogin migrate <authme|nlogin|fastlogin></red>"));
                    }
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>Uso: /smartlogin migrate <authme|nlogin|fastlogin></red>"));
                }
                break;
            default:
                if (sender instanceof Player player) {
                    plugin.getAdminPanelGUI().openPanel(player);
                } else {
                    sendHelp(sender);
                }
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ SMARTLOGIN — ADMINISTRACIÓN ] ━━━━━━━━━━━━</bold></gradient>"));
        sendCmdLine(sender, "/smartlogin gui", "Abre el panel de control interactivo");
        sendCmdLine(sender, "/smartlogin reload", "Recarga configuraciones, 2FA e idiomas");
        sendCmdLine(sender, "/smartlogin setpassword <user> <pass>", "Cambia la clave de cualquier jugador");
        sendCmdLine(sender, "/smartlogin unregister <user>", "Elimina la cuenta de un jugador");
        sendCmdLine(sender, "/smartlogin history <user>", "Auditoría de IPs, fechas y seguridad");
        sendCmdLine(sender, "/smartlogin migrate <authme|nlogin|fastlogin>", "Migra cuentas de otros plugins");
        sendCmdLine(sender, "/smartlogin setspawn", "Define el punto de spawn de autenticación");
        sendCmdLine(sender, "/smartlogin backup", "Crea un respaldo de la base de datos");
        sendCmdLine(sender, "/smartlogin resetsetup", "Reinicia el asistente cinemático");
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(Component.empty());
    }

    private void sendCmdLine(CommandSender sender, String cmd, String desc) {
        Component line = plugin.getLocaleManager().parse("  <#C084FC><bold>" + cmd + "</bold></#C084FC> <dark_gray>—</dark_gray> <gray>" + desc + "</gray>")
                .clickEvent(ClickEvent.suggestCommand(cmd))
                .hoverEvent(HoverEvent.showText(Component.text("Haz clic para sugerir en el chat", NamedTextColor.LIGHT_PURPLE)));
        sender.sendMessage(line);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("smartlogin.admin")) return List.of();

        if (args.length == 1) {
            List<String> subs = Arrays.asList("gui", "help", "reload", "setpassword", "unregister", "history", "migrate", "setspawn", "backup", "resetsetup", "setup");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("migrate") || args[0].equalsIgnoreCase("import"))) {
            return Arrays.asList("authme", "nlogin", "fastlogin").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }

        return List.of();
    }
}

