package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.PasswordHasher;
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
import java.util.Map;
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
                plugin.getSessionShield().clearAllSessions();

                if (sender instanceof Player player) {
                    plugin.getDatabaseManager().deleteProfile(player.getUniqueId());
                    plugin.getDatabaseManager().loadProfileByName(player.getName()).thenAccept(p -> {
                        if (p != null) plugin.getDatabaseManager().deleteProfile(p.getUuid());
                    });
                    plugin.getAuthManager().uncache(player.getUniqueId());
                    plugin.getAuthManager().setAuthenticated(player.getUniqueId(), false);
                }

                Component resetKickMsg = plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient>\n\n<#F5D0FE>El sistema de autenticación ha sido reiniciado por un administrador.\nPor favor vuelve a ingresar.</#F5D0FE>");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.kick(resetKickMsg);
                }
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
            case "linkdiscord":
                if (args.length >= 3) {
                    String code = args[1];
                    String discordId = args[2];
                    boolean ok = plugin.getDiscordManager().completeLink(code, discordId);
                    if (ok) {
                        sender.sendMessage("✔ Cuenta vinculada exitosamente con Discord ID " + discordId);
                    } else {
                        sender.sendMessage("✖ Código de vinculación inválido o expirado: " + code);
                    }
                } else {
                    sender.sendMessage("Uso: /smartlogin linkdiscord <código> <discord_id>");
                }
                break;
            case "unlink":
            case "unlinkdiscord":
                if (args.length > 1) {
                    String targetName = args[1];
                    String method = args.length > 2 ? args[2].toLowerCase() : "discord";
                    plugin.getDatabaseManager().loadProfileByName(targetName).thenAccept(p -> {
                        if (p != null) {
                            boolean changed = false;
                            if (method.equals("discord") || method.equals("dc") || method.equals("all")) {
                                plugin.getDiscordManager().unlinkDiscord(p.getUuid());
                                changed = true;
                            }
                            if (method.equals("email") || method.equals("gmail") || method.equals("all")) {
                                p.setEmail(null);
                                changed = true;
                            }
                            if (method.equals("2fa") || method.equals("totp") || method.equals("all")) {
                                p.set2FAEnabled(false);
                                p.setTotpSecret(null);
                                p.setBackupCodes(null);
                                changed = true;
                            }
                            if (changed) {
                                p.setLastLoginTimestamp(0);
                                plugin.getDatabaseManager().saveProfile(p);
                                plugin.getAuthManager().cacheProfile(p.getUuid(), p);

                                if (method.equals("all")) {
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        Player online = Bukkit.getPlayerExact(targetName);
                                        if (online != null && online.isOnline()) {
                                            plugin.getAuthManager().removeAuthenticated(online.getUniqueId());
                                            online.kick(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient>\n\n<#F5D0FE>Tus métodos de seguridad fueron desvinculados por un administrador.\nPor favor vuelve a ingresar.</#F5D0FE>"));
                                        }
                                    });
                                }

                                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ Se desvinculó [" + method + "] de <#C084FC>" + targetName + "</#C084FC>.</#E9D5FF>"));
                            } else {
                                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Método desconocido. Opciones: discord, email, 2fa, all.</#F5D0FE>"));
                            }
                        } else {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>✖ No se encontró al jugador <#C084FC>" + targetName + "</#C084FC>.</#F5D0FE>"));
                        }
                    });
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /smartlogin unlink <jugador> [discord|email|2fa|all]</#F5D0FE>"));
                }
                break;
            case "reset2fa":
                if (args.length > 1) {
                    String targetName = args[1];
                    plugin.getDatabaseManager().loadProfileByName(targetName).thenAccept(p -> {
                        if (p != null) {
                            plugin.getDiscordManager().unlinkDiscord(p.getUuid());
                            p.setEmail(null);
                            p.set2FAEnabled(false);
                            p.setTotpSecret(null);
                            p.setBackupCodes(null);
                            p.setLastLoginTimestamp(0);
                            plugin.getDatabaseManager().saveProfile(p);
                            plugin.getAuthManager().cacheProfile(p.getUuid(), p);

                            Bukkit.getScheduler().runTask(plugin, () -> {
                                Player online = Bukkit.getPlayerExact(targetName);
                                if (online != null && online.isOnline()) {
                                    plugin.getAuthManager().removeAuthenticated(online.getUniqueId());
                                    online.kick(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient>\n\n<#F5D0FE>Tus métodos de seguridad 2FA fueron restablecidos por un administrador.\nPor favor vuelve a ingresar.</#F5D0FE>"));
                                }
                            });

                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ Todos los métodos 2FA de <#C084FC>" + targetName + "</#C084FC> han sido restablecidos.</#E9D5FF>"));
                        } else {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>✖ No se encontró al jugador <#C084FC>" + targetName + "</#C084FC>.</#F5D0FE>"));
                        }
                    });
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /smartlogin reset2fa <jugador></#F5D0FE>"));
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
                    if (source.equals("all")) {
                        plugin.getMigrationManager().importAll(sender).thenAccept(count -> {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <green><bold>✔ ¡Migración completa finalizada!</bold></green> Se importaron <#C084FC><bold>" + count + "</bold></#C084FC> cuentas en total."));
                        });
                    } else if (source.equals("authme")) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Iniciando migración desde AuthMe (SQLite/MySQL)...</#E9D5FF>"));
                        plugin.getMigrationManager().importAuthMe(sender).thenAccept(count -> {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ ¡Se importaron exitosamente <#C084FC><bold>" + count + "</bold></#C084FC> cuentas desde AuthMe!</#E9D5FF>"));
                        });
                    } else if (source.equals("nlogin")) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Iniciando migración desde nLogin (SQLite/MySQL)...</#E9D5FF>"));
                        plugin.getMigrationManager().importNLogin(sender).thenAccept(count -> {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ ¡Se importaron exitosamente <#C084FC><bold>" + count + "</bold></#C084FC> cuentas desde nLogin!</#E9D5FF>"));
                        });
                    } else if (source.equals("fastlogin")) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Iniciando migración de estados Premium desde FastLogin...</#E9D5FF>"));
                        plugin.getMigrationManager().importFastLogin(sender).thenAccept(count -> {
                            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ ¡Se sincronizaron <#C084FC><bold>" + count + "</bold></#C084FC> cuentas desde FastLogin!</#E9D5FF>"));
                        });
                    } else if (source.equals("crazylogin")) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Iniciando migración desde CrazyLogin...</#E9D5FF>"));
                        plugin.getMigrationManager().importCrazyLogin(sender);
                    } else if (source.equals("xauth")) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Iniciando migración desde xAuth...</#E9D5FF>"));
                        plugin.getMigrationManager().importXAuth(sender);
                    } else if (source.equals("loginsecurity")) {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Iniciando migración desde LoginSecurity...</#E9D5FF>"));
                        plugin.getMigrationManager().importLoginSecurity(sender);
                    } else {
                        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Fuente desconocida. Usa: /smartlogin migrate <all|authme|nlogin|fastlogin|crazylogin|xauth|loginsecurity></#F5D0FE>"));
                    }
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /smartlogin migrate <all|authme|nlogin|fastlogin|crazylogin|xauth|loginsecurity></#F5D0FE>"));
                }
                break;
            case "lang":
            case "language":
            case "idioma":
                if (args.length > 1) {
                    String targetLang = args[1].toLowerCase();
                    boolean ok = plugin.getLocaleManager().setDefaultLanguage(targetLang);
                    if (ok) {
                        // Reload configs and languages to ensure all files and dictionaries are hot-reloaded
                        plugin.getModularConfig().loadAll();
                        plugin.getLocaleManager().loadLanguages();

                        if (sender instanceof Player p) {
                            plugin.getLocaleManager().setPlayerLanguage(p.getUniqueId(), targetLang);
                        }
                        plugin.getServer().getOnlinePlayers().forEach(plugin::refreshPlayerUI);

                        Map<String, String> ph = Map.of("{lang}", targetLang.toUpperCase());
                        Component changedMsg = plugin.getLocaleManager().getComponent("lang-server-changed", sender, ph);
                        sender.sendMessage(plugin.getLocaleManager().parse(plugin.getLocaleManager().getRawMessage("prefix", targetLang)).append(changedMsg));
                        sender.sendMessage(plugin.getLocaleManager().getComponent("admin-reloaded", sender));
                    } else {
                        sender.sendMessage(plugin.getLocaleManager().getComponent("lang-invalid", sender));
                    }
                } else {
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Uso: /smartlogin lang <es|en|pt|fr|de|ru|zh></#F5D0FE>"));
                }
                break;
            case "diagnose":
            case "health":
            case "status":
                plugin.getDiagnosticManager().runDiagnosis(sender);
                break;
            case "benchmark":
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <gray>Ejecutando prueba de velocidad de algoritmos de cifrado...</gray>"));
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    double a2 = PasswordHasher.benchmark("ARGON2ID");
                    double pb = PasswordHasher.benchmark("PBKDF2_SHA512");
                    double bc = PasswordHasher.benchmark("BCRYPT");
                    double sh = PasswordHasher.benchmark("SHA256");
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Benchmark:</bold></gradient>"));
                    sender.sendMessage(plugin.getLocaleManager().parse(" <gray>• Argon2id (64MB RAM):</gray> <#E9D5FF>" + String.format("%.2f", a2) + "ms</#E9D5FF>"));
                    sender.sendMessage(plugin.getLocaleManager().parse(" <gray>• PBKDF2-SHA512 (65k it):</gray> <#E9D5FF>" + String.format("%.2f", pb) + "ms</#E9D5FF>"));
                    sender.sendMessage(plugin.getLocaleManager().parse(" <gray>• BCrypt (Cost 12):</gray> <#E9D5FF>" + String.format("%.2f", bc) + "ms</#E9D5FF>"));
                    sender.sendMessage(plugin.getLocaleManager().parse(" <gray>• SHA-256 (Salted):</gray> <#E9D5FF>" + String.format("%.2f", sh) + "ms</#E9D5FF>"));
                });
                break;
            case "version":
            case "v":
            case "ver":
            case "info":
                sendVersionInfo(sender);
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

    private void sendVersionInfo(CommandSender sender) {
        String serverVer = Bukkit.getVersion();
        String bukkitVer = Bukkit.getBukkitVersion();
        String langCode = plugin.getLocaleManager().getSenderLanguage(sender);
        String clientInfo = (sender instanceof Player p) ? (p.getName() + " (" + plugin.getLocaleManager().getRawMessage("admin-version-user", p) + " " + langCode.toUpperCase() + ")") : plugin.getLocaleManager().getRawMessage("admin-version-user", langCode);

        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().getComponent("admin-version-header", sender));
        sender.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>" + plugin.getLocaleManager().getRawMessage("admin-version-plugin", langCode) + "</#E9D5FF> <#C084FC><bold>v" + plugin.getPluginMeta().getVersion() + "</bold></#C084FC>"));
        sender.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>" + plugin.getLocaleManager().getRawMessage("admin-version-author", langCode) + "</#E9D5FF> <#C084FC><bold>Dafealru</bold></#C084FC>"));
        sender.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>" + plugin.getLocaleManager().getRawMessage("admin-version-platform", langCode) + "</#E9D5FF> <gray>" + bukkitVer + " (" + serverVer + ")</gray>"));
        sender.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>" + plugin.getLocaleManager().getRawMessage("admin-version-compat", langCode) + "</#E9D5FF> <#C084FC>✔ 100% (Paper / Purpur / Spigot 1.21.x)</#C084FC>"));
        sender.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>" + plugin.getLocaleManager().getRawMessage("admin-version-user", langCode) + "</#E9D5FF> <gray>" + clientInfo + "</gray>"));
        sender.sendMessage(Component.empty());

        Component wikiLink = plugin.getLocaleManager().parse("  <#C084FC><bold>" + plugin.getLocaleManager().getRawMessage("admin-version-wiki", langCode) + "</bold></#C084FC> <#E9D5FF><u>https://github.com/dr8553097-sudo/SmartLogin/wiki</u></#E9D5FF>")
                .clickEvent(ClickEvent.openUrl("https://github.com/dr8553097-sudo/SmartLogin/wiki"))
                .hoverEvent(HoverEvent.showText(plugin.getLocaleManager().getComponent("admin-version-click-wiki", sender)));

        Component supportLink = plugin.getLocaleManager().parse("  <#C084FC><bold>" + plugin.getLocaleManager().getRawMessage("admin-version-discord", langCode) + "</bold></#C084FC> <#E9D5FF><u>https://discord.gg/smartlogin</u></#E9D5FF>")
                .clickEvent(ClickEvent.openUrl("https://discord.gg/smartlogin"))
                .hoverEvent(HoverEvent.showText(plugin.getLocaleManager().getComponent("admin-version-click-discord", sender)));

        Component portfolioLink = plugin.getLocaleManager().parse("  <#C084FC><bold>" + plugin.getLocaleManager().getRawMessage("admin-version-portfolio", langCode) + "</bold></#C084FC> <#E9D5FF><u>https://github.com/dr8553097-sudo</u></#E9D5FF>")
                .clickEvent(ClickEvent.openUrl("https://github.com/dr8553097-sudo"))
                .hoverEvent(HoverEvent.showText(plugin.getLocaleManager().getComponent("admin-version-click-portfolio", sender)));

        sender.sendMessage(wikiLink);
        sender.sendMessage(supportLink);
        sender.sendMessage(portfolioLink);
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(Component.empty());
    }

    private void sendHelp(CommandSender sender) {
        String langCode = plugin.getLocaleManager().getSenderLanguage(sender);

        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().getComponent("admin-help-header", sender));
        sendCmdLine(sender, "/smartlogin diagnose", "Ejecutar diagnóstico integral de archivos YAML, latencia y cero lag");
        sendCmdLine(sender, "/smartlogin benchmark", "Prueba de velocidad en tiempo real de los algoritmos de cifrado");
        sendCmdLine(sender, "/smartlogin gui", plugin.getLocaleManager().getRawMessage("admin-help-gui", langCode));
        sendCmdLine(sender, "/smartlogin version", plugin.getLocaleManager().getRawMessage("admin-help-version", langCode));
        sendCmdLine(sender, "/smartlogin reload", plugin.getLocaleManager().getRawMessage("admin-help-reload", langCode));
        sendCmdLine(sender, "/smartlogin lang <es|en|pt|fr|de|ru|zh>", plugin.getLocaleManager().getRawMessage("admin-help-lang", langCode));
        sendCmdLine(sender, "/smartlogin reset2fa <user>", "Restablecer todos los métodos 2FA de un jugador");
        sendCmdLine(sender, "/smartlogin unlink <user> [discord|email|all]", "Desvincular método de seguridad de un jugador");
        sendCmdLine(sender, "/smartlogin setpassword <user> <pass>", plugin.getLocaleManager().getRawMessage("admin-help-setpassword", langCode));
        sendCmdLine(sender, "/smartlogin unregister <user>", plugin.getLocaleManager().getRawMessage("admin-help-unregister", langCode));
        sendCmdLine(sender, "/smartlogin history <user>", plugin.getLocaleManager().getRawMessage("admin-help-history", langCode));
        sendCmdLine(sender, "/smartlogin migrate <authme|nlogin|fastlogin>", plugin.getLocaleManager().getRawMessage("admin-help-migrate", langCode));
        sendCmdLine(sender, "/smartlogin setspawn", plugin.getLocaleManager().getRawMessage("admin-help-setspawn", langCode));
        sendCmdLine(sender, "/smartlogin backup", plugin.getLocaleManager().getRawMessage("admin-help-backup", langCode));
        sendCmdLine(sender, "/smartlogin resetsetup", plugin.getLocaleManager().getRawMessage("admin-help-resetsetup", langCode));
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(Component.empty());
    }

    private void sendCmdLine(CommandSender sender, String cmd, String desc) {
        Component line = plugin.getLocaleManager().parse("  <#C084FC><bold>" + cmd + "</bold></#C084FC> <dark_gray>—</dark_gray> <gray>" + desc + "</gray>")
                .clickEvent(ClickEvent.suggestCommand(cmd))
                .hoverEvent(HoverEvent.showText(Component.text("Click to suggest in chat", NamedTextColor.LIGHT_PURPLE)));
        sender.sendMessage(line);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("smartlogin.admin")) return List.of();

        if (args.length == 1) {
            List<String> subs = Arrays.asList("diagnose", "benchmark", "gui", "help", "version", "reload", "lang", "setpassword", "unregister", "history", "migrate", "setspawn", "backup", "resetsetup", "setup", "unlink", "reset2fa");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("unlink") || args[0].equalsIgnoreCase("unlinkdiscord"))) {
            return Arrays.asList("discord", "email", "2fa", "all").stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("lang") || args[0].equalsIgnoreCase("language") || args[0].equalsIgnoreCase("idioma"))) {
            return Arrays.asList("es", "en", "pt", "fr", "de", "ru", "zh").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("migrate") || args[0].equalsIgnoreCase("import"))) {
            return Arrays.asList("all", "authme", "nlogin", "fastlogin", "crazylogin", "xauth", "loginsecurity").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }

        return List.of();
    }
}

