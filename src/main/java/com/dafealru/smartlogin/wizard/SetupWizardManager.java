package com.dafealru.smartlogin.wizard;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SetupWizardManager {

    private final SmartLogin plugin;
    private final Map<UUID, Integer> adminCurrentStep = new ConcurrentHashMap<>();

    public SetupWizardManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public boolean isSetupCompleted() {
        return plugin.getModularConfig().getConfig().getBoolean("setup-completed", false);
    }

    public boolean isWizardActive(UUID uuid) {
        return adminCurrentStep.containsKey(uuid);
    }

    public int getCurrentStep(UUID uuid) {
        return adminCurrentStep.getOrDefault(uuid, 0);
    }

    public void removeWizardActive(UUID uuid) {
        adminCurrentStep.remove(uuid);
    }

    public void startWizard(Player player) {
        adminCurrentStep.put(player.getUniqueId(), 0);

        // 1. First on-screen title
        Title title1 = Title.title(
                plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient>"),
                plugin.getLocaleManager().parse("<#E9D5FF>¡Gracias por usar SmartLogin!</#E9D5FF>"),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2000), Duration.ofMillis(500))
        );
        player.showTitle(title1);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        // 2. Second on-screen subtitle explanation (after 2.2s)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            Title title2 = Title.title(
                    plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>Configuración Inicial</bold></gradient>"),
                    plugin.getLocaleManager().parse("<#E9D5FF>Asistente interactivo en 6 pasos rápidos</#E9D5FF>"),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2200), Duration.ofMillis(500))
            );
            player.showTitle(title2);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.6f);
        }, 44L);

        // 3. Start step 1 cleanly in chat (after titles finish at 3.5s)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            sendStep(player, 1);
        }, 70L);
    }

    public void sendSetupForm(CommandSender sender) {
        if (sender instanceof Player player) {
            startWizard(player);
        } else {
            sendStep(sender, 1);
        }
    }

    public void sendStep(CommandSender sender, int step) {
        if (sender instanceof Player player) {
            adminCurrentStep.put(player.getUniqueId(), step);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.8f);
        }

        switch (step) {
            case 1 -> sendStep1(sender);
            case 2 -> sendStep2(sender);
            case 3 -> sendStep3(sender);
            case 4 -> sendStep4(sender);
            case 5 -> sendStep5(sender);
            case 6 -> sendStep6(sender);
            default -> finishSetup(sender);
        }
    }

    // PASO 1: BASE DE DATOS
    private void sendStep1(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF><bold>Paso 1/6:</bold> ¿Dónde deseas guardar las cuentas y datos?</#E9D5FF>"));

        Component sqliteBtn = plugin.getLocaleManager().parse("  <gradient:#A855F7:#C084FC><bold>[ 📂 SQLite (Local) ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Recomendado para servidores individuales (smartlogin.db en WAL mode).", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 1 sqlite"));

        Component mysqlBtn = plugin.getLocaleManager().parse("  <gradient:#7C3AED:#9333EA><bold>[ 🌐 MySQL (Remoto) ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Para networks con múltiples lobbies sincronizados vía HikariCP.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 1 mysql"));

        sender.sendMessage(sqliteBtn.append(Component.text("   ")).append(mysqlBtn));
        sender.sendMessage(Component.empty());
    }

    // PASO 2: ALGORITMO DE CIFRADO DE CONTRASEÑAS
    private void sendStep2(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF><bold>Paso 2/6:</bold> Elige el nivel de cifrado de contraseñas:</#E9D5FF>"));

        Component argonBtn = plugin.getLocaleManager().parse("  <gradient:#10B981:#34D399><bold>[ 🛡️ Argon2id (Recomendado / OWASP) ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Máximo nivel militar. Resistente a granjas de GPU y cracking masivo (64 MB RAM, 3 pases).", NamedTextColor.GREEN)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 2 argon2id"));

        Component pbkdfBtn = plugin.getLocaleManager().parse("  <gradient:#A855F7:#C084FC><bold>[ ⚡ PBKDF2-SHA512 ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Equilibrado: 65,536 iteraciones con SHA-512 de alta velocidad.", NamedTextColor.LIGHT_PURPLE)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 2 pbkdf2"));

        Component bcryptBtn = plugin.getLocaleManager().parse("  <gradient:#3B82F6:#60A5FA><bold>[ 🔒 BCrypt ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Estándar web clásico (Cost=12).", NamedTextColor.BLUE)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 2 bcrypt"));

        sender.sendMessage(argonBtn);
        sender.sendMessage(pbkdfBtn.append(Component.text("   ")).append(bcryptBtn));
        sender.sendMessage(Component.empty());
    }

    // PASO 3: COMPATIBILIDAD BEDROCK / GEYSER
    private void sendStep3(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF><bold>Paso 3/6:</bold> ¿Tu servidor admite jugadores Bedrock (GeyserMC / Floodgate)?</#E9D5FF>"));

        Component yesBtn = plugin.getLocaleManager().parse("  <gradient:#A855F7:#C084FC><bold>[ ✔ SÍ (Auto-Login Bedrock) ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Los jugadores de Bedrock entran automáticamente sin pedir contraseña.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 3 on"));

        Component noBtn = plugin.getLocaleManager().parse("  <dark_gray><bold>[ ✖ NO (Solo Java) ]</bold></dark_gray>")
                .hoverEvent(HoverEvent.showText(Component.text("Desactiva la detección de Floodgate/Bedrock.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 3 off"));

        sender.sendMessage(yesBtn.append(Component.text("   ")).append(noBtn));
        sender.sendMessage(Component.empty());
    }

    // PASO 4: PROXY / REDIRECCIÓN A LOBBY
    private void sendStep4(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF><bold>Paso 4/6:</bold> ¿Este servidor es un Auth detrás de Proxy o es Standalone?</#E9D5FF>"));

        Component proxyBtn = plugin.getLocaleManager().parse("  <gradient:#A855F7:#C084FC><bold>[ 🚀 Proxy / BungeeCord / Velocity ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Envía al jugador automáticamente al Lobby al autenticarse.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 4 proxy"));

        Component standaloneBtn = plugin.getLocaleManager().parse("  <gradient:#7C3AED:#9333EA><bold>[ 🏠 Servidor Individual ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("El jugador se queda jugando en este mismo servidor tras loguearse.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 4 standalone"));

        sender.sendMessage(proxyBtn.append(Component.text("   ")).append(standaloneBtn));
        sender.sendMessage(Component.empty());
    }

    // PASO 5: SUITE 2FA
    private void sendStep5(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF><bold>Paso 5/6:</bold> ¿Deseas habilitar la suite 2FA (Google Auth con mapa QR)?</#E9D5FF>"));

        Component enable2fa = plugin.getLocaleManager().parse("  <gradient:#A855F7:#C084FC><bold>[ 🔐 Habilitar 2FA ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Permite /2fa setup con mapa QR en el juego y códigos de recuperación.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 5 on"));

        Component disable2fa = plugin.getLocaleManager().parse("  <dark_gray><bold>[ ✖ Desactivar ]</bold></dark_gray>")
                .hoverEvent(HoverEvent.showText(Component.text("Desactiva los módulos de doble factor.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 5 off"));

        sender.sendMessage(enable2fa.append(Component.text("   ")).append(disable2fa));
        sender.sendMessage(Component.empty());
    }

    // PASO 6: SESSIONSHIELD
    private void sendStep6(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF><bold>Paso 6/6:</bold> ¿Deseas activar SessionShield (Reconexión rápida sin clave en misma IP)?</#E9D5FF>"));

        Component enableShield = plugin.getLocaleManager().parse("  <gradient:#A855F7:#C084FC><bold>[ ⚡ Activar SessionShield (Recomendado) ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Permite a los jugadores reconectarse sin escribir contraseña si su IP es la misma.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 6 on"));

        Component disableShield = plugin.getLocaleManager().parse("  <dark_gray><bold>[ 🔒 Desactivar (Pedir clave siempre) ]</bold></dark_gray>")
                .hoverEvent(HoverEvent.showText(Component.text("Exige contraseña en cada conexión sin excepción.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 6 off"));

        sender.sendMessage(enableShield.append(Component.text("   ")).append(disableShield));
        sender.sendMessage(Component.empty());
    }

    public void handleStepChoice(CommandSender sender, int step, String choice) {
        FileConfiguration authConfig = plugin.getModularConfig().getAuthConfig();
        FileConfiguration dbConfig = plugin.getModularConfig().getDatabaseConfig();
        FileConfiguration totpConfig = plugin.getModularConfig().getTotpConfig();

        switch (step) {
            case 1 -> {
                if ("mysql".equalsIgnoreCase(choice)) {
                    dbConfig.set("type", "MYSQL");
                    plugin.getModularConfig().saveDatabase();
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <gray>Almacenamiento: <#C084FC>MySQL / MariaDB</#C084FC></gray>"));
                } else {
                    dbConfig.set("type", "SQLITE");
                    plugin.getModularConfig().saveDatabase();
                    sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <gray>Almacenamiento: <#C084FC>SQLite (Local)</#C084FC></gray>"));
                }
                sendStep(sender, 2);
            }
            case 2 -> {
                String algo = switch (choice.toLowerCase()) {
                    case "pbkdf2" -> "PBKDF2_SHA512";
                    case "bcrypt" -> "BCRYPT";
                    case "sha256" -> "SHA256";
                    default -> "ARGON2ID";
                };
                authConfig.set("hashing.algorithm", algo);
                plugin.getModularConfig().saveAuth();
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <gray>Cifrado seleccionado: <#C084FC><bold>" + algo + "</bold></#C084FC></gray>"));
                sendStep(sender, 3);
            }
            case 3 -> {
                boolean bedrockOn = "on".equalsIgnoreCase(choice);
                authConfig.set("bedrock.auto-login-enabled", bedrockOn);
                plugin.getModularConfig().saveAuth();
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <gray>Soporte Bedrock: <#C084FC>" + (bedrockOn ? "ACTIVADO" : "DESACTIVADO") + "</#C084FC></gray>"));
                sendStep(sender, 4);
            }
            case 4 -> {
                boolean proxyOn = "proxy".equalsIgnoreCase(choice);
                authConfig.set("proxy.send-to-lobby-on-login", proxyOn);
                plugin.getModularConfig().saveAuth();
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <gray>Modo de Red: <#C084FC>" + (proxyOn ? "Proxy (Redirección)" : "Standalone (Individual)") + "</#C084FC></gray>"));
                sendStep(sender, 5);
            }
            case 5 -> {
                boolean twoFaOn = "on".equalsIgnoreCase(choice);
                totpConfig.set("enabled", twoFaOn);
                plugin.getModularConfig().saveTotp();
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <gray>Suite 2FA: <#C084FC>" + (twoFaOn ? "ACTIVADA" : "DESACTIVADA") + "</#C084FC></gray>"));
                sendStep(sender, 6);
            }
            case 6 -> {
                boolean shieldOn = "on".equalsIgnoreCase(choice);
                authConfig.set("session-shield.enabled", shieldOn);
                plugin.getModularConfig().saveAuth();
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <gray>SessionShield: <#C084FC>" + (shieldOn ? "ACTIVADO" : "DESACTIVADO") + "</#C084FC></gray>"));
                finishSetup(sender);
            }
            default -> finishSetup(sender);
        }
    }

    public void handleToggle(CommandSender sender, String feature) {
        FileConfiguration config = plugin.getModularConfig().getConfig();
        FileConfiguration authConfig = plugin.getModularConfig().getAuthConfig();
        FileConfiguration totpConfig = plugin.getModularConfig().getTotpConfig();
        FileConfiguration discordConfig = plugin.getModularConfig().getDiscordConfig();
        FileConfiguration telegramConfig = plugin.getModularConfig().getTelegramConfig();

        switch (feature.toLowerCase()) {
            case "autolang" -> {
                boolean curLang = config.getBoolean("general.auto-detect-client-language", true);
                config.set("general.auto-detect-client-language", !curLang);
                plugin.getModularConfig().saveConfig();
            }
            case "hashmode" -> {
                String cur = authConfig.getString("hashing.algorithm", "ARGON2ID");
                authConfig.set("hashing.algorithm", "ARGON2ID".equalsIgnoreCase(cur) ? "PBKDF2_SHA512" : "ARGON2ID");
                plugin.getModularConfig().saveAuth();
            }
            case "bedrock" -> {
                boolean b = authConfig.getBoolean("bedrock.auto-login-enabled", true);
                authConfig.set("bedrock.auto-login-enabled", !b);
                plugin.getModularConfig().saveAuth();
            }
            case "premium" -> {
                boolean p = authConfig.getBoolean("premium.auto-login-enabled", true);
                authConfig.set("premium.auto-login-enabled", !p);
                plugin.getModularConfig().saveAuth();
            }
            case "2fa" -> {
                boolean t = totpConfig.getBoolean("enabled", true);
                totpConfig.set("enabled", !t);
                plugin.getModularConfig().saveTotp();
            }
            case "staff2fa" -> {
                boolean s = totpConfig.getBoolean("staff-enforcement.enabled", true);
                totpConfig.set("staff-enforcement.enabled", !s);
                plugin.getModularConfig().saveTotp();
            }
            case "discord" -> {
                boolean d = discordConfig.getBoolean("enabled", false);
                discordConfig.set("enabled", !d);
                plugin.getModularConfig().saveDiscord();
            }
            case "telegram" -> {
                boolean tg = telegramConfig.getBoolean("enabled", false);
                telegramConfig.set("enabled", !tg);
                plugin.getModularConfig().saveTelegram();
            }
            case "shield", "sessionshield" -> {
                boolean sh = authConfig.getBoolean("session-shield.enabled", true);
                authConfig.set("session-shield.enabled", !sh);
                plugin.getModularConfig().saveAuth();
            }
            case "nick", "nickname" -> {
                boolean n = config.getBoolean("nickname-protection.strict-case", true);
                config.set("nickname-protection.strict-case", !n);
                plugin.getModularConfig().saveConfig();
            }
            case "captcha" -> {
                int c = config.getInt("captcha.trigger-after-failed-attempts", 2);
                config.set("captcha.trigger-after-failed-attempts", c > 0 ? 0 : 2);
                plugin.getModularConfig().saveConfig();
            }
            case "geo", "geoprotection" -> {
                boolean g = config.getBoolean("geo-protection.enabled", true);
                config.set("geo-protection.enabled", !g);
                plugin.getModularConfig().saveConfig();
            }
            case "spawn", "authspawn" -> {
                boolean sp = config.getBoolean("auth-spawn.enabled", false);
                config.set("auth-spawn.enabled", !sp);
                plugin.getModularConfig().saveConfig();
            }
            default -> sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Función desconocida: " + feature + "</#F5D0FE>"));
        }
    }

    public void finishSetup(CommandSender sender) {
        plugin.getModularConfig().getConfig().set("setup-completed", true);
        plugin.getModularConfig().saveConfig();

        // Invalidate active session tokens
        plugin.getSessionShield().clearAllSessions();

        // Expel other online players to ensure clean re-auth under new setup
        Component kickMsg = plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient>\n\n<#F5D0FE>La configuración del servidor ha sido actualizada por un administrador.\nPor favor vuelve a ingresar.</#F5D0FE>");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player admin && p.getUniqueId().equals(admin.getUniqueId())) {
                continue; // Do not kick the administrator performing the wizard
            }
            p.kick(kickMsg);
        }

        if (sender instanceof Player player) {
            Title finishTitle = Title.title(
                    plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>¡Configuración Lista!</bold></gradient>"),
                    plugin.getLocaleManager().parse("<#E9D5FF>Ajustes guardados correctamente</#E9D5FF>"),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500))
            );
            player.showTitle(finishTitle);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            adminCurrentStep.remove(player.getUniqueId());

            player.sendMessage(Component.empty());
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ ¡Configuración básica inicial completada con éxito!</#E9D5FF>"));

            if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                plugin.getDatabaseManager().loadProfile(player.getUniqueId()).thenCompose(p -> {
                    if (p != null) return java.util.concurrent.CompletableFuture.completedFuture(p);
                    return plugin.getDatabaseManager().loadProfileByName(player.getName());
                }).thenAccept(profile -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!player.isOnline() || plugin.getAuthManager().isAuthenticated(player.getUniqueId())) return;
                        if (profile == null || profile.getPasswordHash() == null || profile.getPasswordHash().trim().isEmpty()) {
                            plugin.getAuthHudManager().startHud(player, true);
                            player.sendMessage(Component.empty());
                            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Por favor regístrate en el servidor usando: <#C084FC><bold>/register <contraseña> <repetir></bold></#C084FC></#E9D5FF>"));
                        } else {
                            plugin.getAuthHudManager().startHud(player, false);
                            player.sendMessage(Component.empty());
                            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Por favor inicia sesión usando: <#C084FC><bold>/login <contraseña></bold></#C084FC></#E9D5FF>"));
                        }
                    });
                });
            } else {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Abre el panel en cualquier momento con </#E9D5FF><#C084FC><click:run_command:/smartlogin gui>/smartlogin gui</click></#C084FC>"));
            }
            player.sendMessage(Component.empty());
        } else {
            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ ¡Configuración básica inicial completada con éxito!</#E9D5FF>"));
        }
    }
}
