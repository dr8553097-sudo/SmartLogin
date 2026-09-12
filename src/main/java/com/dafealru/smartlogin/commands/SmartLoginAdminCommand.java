package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
                if (sender instanceof Player player) {
                    plugin.getDatabaseManager().deleteProfile(player.getUniqueId());
                    plugin.getDatabaseManager().loadProfileByName(player.getName()).thenAccept(p -> {
                        if (p != null) plugin.getDatabaseManager().deleteProfile(p.getUuid());
                    });
                    plugin.getAuthManager().uncache(player.getUniqueId());
                    plugin.getAuthManager().setAuthenticated(player.getUniqueId(), false);
                }
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ Estado del setup y cuenta reiniciados. ¡El asistente cinemático y registro se iniciarán al entrar!</#E9D5FF>"));
                break;
            case "finishsetup":
                plugin.getSetupWizardManager().finishSetup(sender);
                break;
            case "setspawn":
                if (sender instanceof Player player) {
                    plugin.getSpawnManager().setAuthSpawn(player.getLocation());
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ Ubicación del spawn de autenticación establecida en tu posición.</#E9D5FF>"));
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Solo los jugadores pueden ejecutar /smartlogin setspawn.</#F5D0FE>"));
                }
                break;
            case "history":
            case "audit":
                if (args.length > 1) {
                    plugin.getAuditManager().showPlayerHistory(sender, args[1]);
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /smartlogin history <usuario></#F5D0FE>"));
                }
                break;
            case "setpassword":
            case "changepass":
                if (args.length > 2) {
                    String target = args[1];
                    String newPass = args[2];
                    plugin.getAuditManager().setPlayerPassword(target, newPass).thenAccept(ok -> {
                        if (ok) {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ Contraseña de <#C084FC>" + target + "</#C084FC> actualizada correctamente.</#E9D5FF>"));
                        } else {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>✖ No se encontró al jugador en la base de datos.</#F5D0FE>"));
                        }
                    });
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /smartlogin setpassword <usuario> <nueva_clave></#F5D0FE>"));
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
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    Player online = Bukkit.getPlayerExact(targetName);
                                    if (online != null && online.isOnline()) {
                                        online.kick(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient>\n\n<#F5D0FE>Tu cuenta ha sido eliminada por un administrador.</#F5D0FE>"));
                                    }
                                });
                                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ Cuenta de <#C084FC>" + targetName + "</#C084FC> eliminada correctamente.</#E9D5FF>"));
                            });
                        } else {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                Player online = Bukkit.getPlayerExact(targetName);
                                if (online != null && online.isOnline()) {
                                    online.kick(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient>\n\n<#F5D0FE>Tu cuenta ha sido eliminada por un administrador.</#F5D0FE>"));
                                }
                            });
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>✖ No se encontró al jugador <#C084FC>" + targetName + "</#C084FC> en la base de datos.</#F5D0FE>"));
                        }
                    });
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /smartlogin unregister <usuario></#F5D0FE>"));
                }
                break;
            case "backup":
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Creando copia de seguridad de la base de datos...</#E9D5FF>"));
                File backupFile = plugin.getAuditManager().createDatabaseBackup();
                if (backupFile != null) {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ Copia de seguridad creada: <#C084FC>" + backupFile.getName() + "</#C084FC></#E9D5FF>"));
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>✖ Error creando la copia de seguridad.</#F5D0FE>"));
                }
                break;
            case "reload":
                plugin.getModularConfig().loadAll();
                plugin.getLocaleManager().loadLanguages();
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ Configuraciones modulares, suite 2FA e idiomas recargados correctamente.</#E9D5FF>"));
                break;
            case "migrate":
            case "import":
                if (args.length > 1) {
                    String source = args[1].toLowerCase();
                    if (source.equals("authme")) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Iniciando migración desde AuthMe...</#E9D5FF>"));
                        plugin.getMigrationManager().importAuthMe(sender).thenAccept(count -> {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ ¡Se importaron exitosamente <#C084FC><bold>" + count + "</bold></#C084FC> cuentas desde AuthMe!</#E9D5FF>"));
                        });
                    } else if (source.equals("nlogin")) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Iniciando migración desde nLogin...</#E9D5FF>"));
                        plugin.getMigrationManager().importNLogin(sender).thenAccept(count -> {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ ¡Se importaron exitosamente <#C084FC><bold>" + count + "</bold></#C084FC> cuentas desde nLogin!</#E9D5FF>"));
                        });
                    } else if (source.equals("fastlogin")) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Iniciando migración de estados Premium desde FastLogin...</#E9D5FF>"));
                        plugin.getMigrationManager().importFastLogin(sender).thenAccept(count -> {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ ¡Se sincronizaron <#C084FC><bold>" + count + "</bold></#C084FC> cuentas desde FastLogin!</#E9D5FF>"));
                        });
                    } else {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Fuente desconocida. Usa: /smartlogin migrate <authme|nlogin|fastlogin></#F5D0FE>"));
                    }
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /smartlogin migrate <authme|nlogin|fastlogin></#F5D0FE>"));
                }
                break;
            case "lang":
            case "language":
            case "idioma":
                if (args.length > 1) {
                    String targetLang = args[1].toLowerCase();
                    boolean ok = plugin.getLocaleManager().setDefaultLanguage(targetLang);
                    if (ok) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ Idioma predeterminado cambiado a: <#C084FC><bold>" + targetLang.toUpperCase() + "</bold></#C084FC></#E9D5FF>"));
                    } else {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>✖ Idioma no reconocido. Disponibles: <#C084FC>es, en, pt, fr, de, ru, zh</#C084FC></#F5D0FE>"));
                    }
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /smartlogin lang <es|en|pt|fr|de|ru|zh></#F5D0FE>"));
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
        sendCmdLine(sender, "/smartlogin lang <es|en|pt|fr|de|ru|zh>", "Cambia el idioma global del plugin");
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
            List<String> subs = Arrays.asList("gui", "help", "reload", "lang", "setpassword", "unregister", "history", "migrate", "setspawn", "backup", "resetsetup", "setup");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("lang") || args[0].equalsIgnoreCase("language") || args[0].equalsIgnoreCase("idioma"))) {
            return Arrays.asList("es", "en", "pt", "fr", "de", "ru", "zh").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("migrate") || args[0].equalsIgnoreCase("import"))) {
            return Arrays.asList("authme", "nlogin", "fastlogin").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }

        return List.of();
    }
}

