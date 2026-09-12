package com.dafealru.smartlogin.wizard;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

    public void startWizard(Player player) {
        adminCurrentStep.put(player.getUniqueId(), 1);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("╔══════════════════════════════════════════════════╗", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  ⚡ ¡BIENVENIDO A SMARTLOGIN SUITE v1.0.0!", NamedTextColor.YELLOW, TextDecoration.BOLD));
        player.sendMessage(Component.text("  Detectamos que eres Administrador / OP y es la", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  primera vez que se inicia el sistema de autenticación.", NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("  A continuación iniciaremos la configuración básica", NamedTextColor.AQUA));
        player.sendMessage(Component.text("  paso a paso. Haz clic en tu opción preferida:", NamedTextColor.AQUA));
        player.sendMessage(Component.text("╚══════════════════════════════════════════════════╝", NamedTextColor.GOLD));

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sendStep(player, 1);
        }, 20L);
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
            case 1:
                sendStep1(sender);
                break;
            case 2:
                sendStep2(sender);
                break;
            case 3:
                sendStep3(sender);
                break;
            case 4:
                sendStep4(sender);
                break;
            case 5:
                sendStep5(sender);
                break;
            default:
                finishSetup(sender);
                break;
        }
    }

    // PASO 1: BASE DE DATOS
    private void sendStep1(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━ [ PASO 1 / 5: BASE DE DATOS ] ━━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("¿Dónde deseas guardar las cuentas y datos de los jugadores?", NamedTextColor.YELLOW));
        sender.sendMessage(Component.empty());

        Component sqliteBtn = Component.text("  [ 📂 SQLite (Local / Recomendado) ]  ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Recomendado para servidores individuales (smartlogin.db en WAL mode).", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 1 sqlite"));

        Component mysqlBtn = Component.text("  [ 🌐 MySQL / MariaDB (Networks) ]  ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Para networks con múltiples lobbies sincronizados vía HikariCP.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 1 mysql"));

        sender.sendMessage(sqliteBtn);
        sender.sendMessage(Component.empty());
        sender.sendMessage(mysqlBtn);
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    }

    // PASO 2: COMPATIBILIDAD BEDROCK / GEYSER
    private void sendStep2(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("━━━━━━━━━━━━ [ PASO 2 / 5: SOPORTE BEDROCK ] ━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("¿Tu servidor admite jugadores de Bedrock (GeyserMC / Floodgate)?", NamedTextColor.YELLOW));
        sender.sendMessage(Component.empty());

        Component yesBtn = Component.text("  [ ✔ SÍ (Auto-Login Bedrock Activado) ]  ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Los jugadores de Bedrock entran automáticamente sin pedir contraseña.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 2 on"));

        Component noBtn = Component.text("  [ ✖ NO (Solo Servidor Java) ]  ", NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Desactiva la detección de Floodgate/Bedrock.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 2 off"));

        sender.sendMessage(yesBtn);
        sender.sendMessage(Component.empty());
        sender.sendMessage(noBtn);
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    }

    // PASO 3: PROXY / REDIRECCIÓN A LOBBY
    private void sendStep3(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("━━━━━━━━━━━━━ [ PASO 3 / 5: MODO DE RED ] ━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("¿Este servidor es un Auth detrás de un Proxy (Velocity/Bungee) o es Standalone?", NamedTextColor.YELLOW));
        sender.sendMessage(Component.empty());

        Component proxyBtn = Component.text("  [ 🚀 Servidor Proxy (Redirigir a Lobby) ]  ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Envía al jugador automáticamente al Lobby al autenticarse.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 3 proxy"));

        Component standaloneBtn = Component.text("  [ 🏠 Servidor Individual (Standalone) ]  ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("El jugador se queda jugando en este mismo servidor tras loguearse.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 3 standalone"));

        sender.sendMessage(proxyBtn);
        sender.sendMessage(Component.empty());
        sender.sendMessage(standaloneBtn);
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    }

    // PASO 4: SUITE 2FA
    private void sendStep4(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("━━━━━━━━━━━━━ [ PASO 4 / 5: SUITE 2FA ] ━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("¿Deseas habilitar la suite 2FA (Google Auth con mapa QR, Discord, Telegram)?", NamedTextColor.YELLOW));
        sender.sendMessage(Component.empty());

        Component enable2fa = Component.text("  [ 🔐 Habilitar 2FA Suite (Recomendado) ]  ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Permite /2fa setup con mapa QR en el juego y códigos de recuperación.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 4 on"));

        Component disable2fa = Component.text("  [ ✖ Desactivar 2FA ]  ", NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Desactiva los módulos de doble factor.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 4 off"));

        sender.sendMessage(enable2fa);
        sender.sendMessage(Component.empty());
        sender.sendMessage(disable2fa);
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    }

    // PASO 5: IDIOMA
    private void sendStep5(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("━━━━━━━━━━━━━ [ PASO 5 / 5: IDIOMA ] ━━━━━━━━━━━━━", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("¿Cómo deseas manejar el idioma de los mensajes?", NamedTextColor.YELLOW));
        sender.sendMessage(Component.empty());

        Component autoBtn = Component.text("  [ 🌐 Auto-Detectar Idioma del Cliente ]  ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Muestra los mensajes automáticamente en Español, Inglés o Portugués según el Minecraft del jugador.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 5 auto"));

        Component esBtn = Component.text("  [ 🇪🇸 Fijar Español (Global) ]  ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Todos los jugadores verán siempre los mensajes en Español.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 5 es"));

        Component enBtn = Component.text("  [ 🇺🇸 Fijar Inglés (Global) ]  ", NamedTextColor.AQUA, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Todos los jugadores verán los mensajes en Inglés.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 5 en"));

        sender.sendMessage(autoBtn);
        sender.sendMessage(Component.empty());
        sender.sendMessage(esBtn);
        sender.sendMessage(Component.empty());
        sender.sendMessage(enBtn);
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    }

    public void handleStepChoice(CommandSender sender, int step, String choice) {
        FileConfiguration config = plugin.getModularConfig().getConfig();
        FileConfiguration authConfig = plugin.getModularConfig().getAuthConfig();
        FileConfiguration dbConfig = plugin.getModularConfig().getDatabaseConfig();
        FileConfiguration totpConfig = plugin.getModularConfig().getTotpConfig();

        switch (step) {
            case 1:
                if ("mysql".equalsIgnoreCase(choice)) {
                    dbConfig.set("type", "MYSQL");
                    plugin.getModularConfig().saveDatabase();
                    sender.sendMessage(Component.text("✔ Almacenamiento configurado en: MySQL / MariaDB", NamedTextColor.GREEN));
                } else {
                    dbConfig.set("type", "SQLITE");
                    plugin.getModularConfig().saveDatabase();
                    sender.sendMessage(Component.text("✔ Almacenamiento configurado en: SQLite (Local)", NamedTextColor.GREEN));
                }
                sendStep(sender, 2);
                break;

            case 2:
                boolean bedrockOn = "on".equalsIgnoreCase(choice);
                authConfig.set("bedrock.auto-login-enabled", bedrockOn);
                plugin.getModularConfig().saveAuth();
                sender.sendMessage(Component.text("✔ Soporte Bedrock: " + (bedrockOn ? "ACTIVADO" : "DESACTIVADO"), NamedTextColor.GREEN));
                sendStep(sender, 3);
                break;

            case 3:
                boolean proxyOn = "proxy".equalsIgnoreCase(choice);
                authConfig.set("proxy.send-to-lobby-on-login", proxyOn);
                plugin.getModularConfig().saveAuth();
                sender.sendMessage(Component.text("✔ Modo de Red: " + (proxyOn ? "Proxy (Redirección a Lobby)" : "Standalone (Individual)"), NamedTextColor.GREEN));
                sendStep(sender, 4);
                break;

            case 4:
                boolean twoFaOn = "on".equalsIgnoreCase(choice);
                totpConfig.set("enabled", twoFaOn);
                plugin.getModularConfig().saveTotp();
                sender.sendMessage(Component.text("✔ Suite 2FA: " + (twoFaOn ? "ACTIVADA" : "DESACTIVADA"), NamedTextColor.GREEN));
                sendStep(sender, 5);
                break;

            case 5:
                if ("auto".equalsIgnoreCase(choice)) {
                    config.set("general.auto-detect-client-language", true);
                    sender.sendMessage(Component.text("✔ Detección automática de idioma del cliente: ACTIVADA", NamedTextColor.GREEN));
                } else if ("es".equalsIgnoreCase(choice)) {
                    config.set("general.auto-detect-client-language", false);
                    config.set("general.default-language", "es");
                    sender.sendMessage(Component.text("✔ Idioma fijado en: ESPAÑOL", NamedTextColor.GREEN));
                } else if ("en".equalsIgnoreCase(choice)) {
                    config.set("general.auto-detect-client-language", false);
                    config.set("general.default-language", "en");
                    sender.sendMessage(Component.text("✔ Idioma fijado en: INGLÉS", NamedTextColor.GREEN));
                }
                plugin.getModularConfig().saveConfig();
                plugin.getLocaleManager().loadLanguages();
                finishSetup(sender);
                break;

            default:
                finishSetup(sender);
                break;
        }
    }

    public void handleToggle(CommandSender sender, String feature) {
        FileConfiguration config = plugin.getModularConfig().getConfig();
        FileConfiguration authConfig = plugin.getModularConfig().getAuthConfig();
        FileConfiguration totpConfig = plugin.getModularConfig().getTotpConfig();

        switch (feature.toLowerCase()) {
            case "autolang":
                boolean curLang = config.getBoolean("general.auto-detect-client-language", true);
                config.set("general.auto-detect-client-language", !curLang);
                plugin.getModularConfig().saveConfig();
                break;
            case "hashmode":
                String curMode = authConfig.getString("hashing.mode", "FAST_PBKDF2");
                authConfig.set("hashing.mode", "FAST_PBKDF2".equalsIgnoreCase(curMode) ? "SECURE_PBKDF2" : "FAST_PBKDF2");
                plugin.getModularConfig().saveAuth();
                break;
            case "bedrock":
                boolean b = authConfig.getBoolean("bedrock.auto-login-enabled", true);
                authConfig.set("bedrock.auto-login-enabled", !b);
                plugin.getModularConfig().saveAuth();
                break;
            case "premium":
                boolean p = authConfig.getBoolean("premium.auto-login-enabled", true);
                authConfig.set("premium.auto-login-enabled", !p);
                plugin.getModularConfig().saveAuth();
                break;
            case "2fa":
                boolean t = totpConfig.getBoolean("enabled", true);
                totpConfig.set("enabled", !t);
                plugin.getModularConfig().saveTotp();
                break;
            case "staff2fa":
                boolean s = totpConfig.getBoolean("staff-enforcement.enabled", true);
                totpConfig.set("staff-enforcement.enabled", !s);
                plugin.getModularConfig().saveTotp();
                break;
            default:
                sender.sendMessage(Component.text("Unknown feature toggle: " + feature, NamedTextColor.RED));
                return;
        }
    }

    public void finishSetup(CommandSender sender) {
        plugin.getModularConfig().getConfig().set("setup-completed", true);
        plugin.getModularConfig().saveConfig();

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("╔══════════════════════════════════════════════════╗", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  🎉 ¡CONFIGURACIÓN INICIAL COMPLETADA CON ÉXITO!", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("  Todos los ajustes han sido guardados correctamente.", NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  👉 Panel de Control GUI: ", NamedTextColor.YELLOW)
                .append(Component.text("/smartlogin gui", NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand("/smartlogin gui"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click para abrir el panel de control")))));
        sender.sendMessage(Component.text("  👉 Ver todos los comandos: ", NamedTextColor.YELLOW)
                .append(Component.text("/smartlogin help", NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand("/smartlogin help"))));
        sender.sendMessage(Component.text("╚══════════════════════════════════════════════════╝", NamedTextColor.GREEN));

        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            Title doneTitle = Title.title(
                    plugin.getLocaleManager().parse("<green><bold>✔ ¡SETUP COMPLETADO!</bold></green>"),
                    plugin.getLocaleManager().parse("<gold>SmartLogin Suite v1.0.0 Listo</gold>"),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))
            );
            player.showTitle(doneTitle);
            adminCurrentStep.remove(player.getUniqueId());
        }
    }
}
