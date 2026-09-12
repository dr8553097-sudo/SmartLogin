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
        player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>╔══════════════════════════════════════════════════╗</bold></gradient>"));
        player.sendMessage(plugin.getLocaleManager().parse("  <light_purple><bold>⚡ SMARTLOGIN SUITE — CONFIGURACIÓN INICIAL</bold></light_purple>"));
        player.sendMessage(plugin.getLocaleManager().parse("  <gray>Detectamos que eres Administrador y es la primera vez</gray>"));
        player.sendMessage(plugin.getLocaleManager().parse("  <gray>que se inicia el sistema de autenticación en este servidor.</gray>"));
        player.sendMessage(Component.empty());
        player.sendMessage(plugin.getLocaleManager().parse("  <#C084FC>Iniciaremos una configuración guiada paso a paso.</#C084FC>"));
        player.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>Haz clic sobre la opción que prefieras en el chat:</#E9D5FF>"));
        player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>╚══════════════════════════════════════════════════╝</bold></gradient>"));

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sendStep(player, 1);
        }, 15L);
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
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━ [ PASO 1 / 5: BASE DE DATOS ] ━━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(plugin.getLocaleManager().parse("<#E9D5FF>¿Dónde deseas guardar las cuentas y datos de los jugadores?</#E9D5FF>"));
        sender.sendMessage(Component.empty());

        Component sqliteBtn = plugin.getLocaleManager().parse("  <gradient:#A855F7:#C084FC><bold>[ 📂 SQLite (Local / Recomendado) ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Recomendado para servidores individuales (smartlogin.db en WAL mode).", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 1 sqlite"));

        Component mysqlBtn = plugin.getLocaleManager().parse("  <gradient:#7C3AED:#9333EA><bold>[ 🌐 MySQL / MariaDB (Networks) ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Para networks con múltiples lobbies sincronizados vía HikariCP.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 1 mysql"));

        sender.sendMessage(sqliteBtn);
        sender.sendMessage(Component.empty());
        sender.sendMessage(mysqlBtn);
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
    }

    // PASO 2: COMPATIBILIDAD BEDROCK / GEYSER
    private void sendStep2(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ PASO 2 / 5: SOPORTE BEDROCK ] ━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(plugin.getLocaleManager().parse("<#E9D5FF>¿Tu servidor admite jugadores de Bedrock (GeyserMC / Floodgate)?</#E9D5FF>"));
        sender.sendMessage(Component.empty());

        Component yesBtn = plugin.getLocaleManager().parse("  <gradient:#A855F7:#C084FC><bold>[ ✔ SÍ (Auto-Login Bedrock Activado) ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Los jugadores de Bedrock entran automáticamente sin pedir contraseña.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 2 on"));

        Component noBtn = plugin.getLocaleManager().parse("  <dark_gray><bold>[ ✖ NO (Solo Servidor Java) ]</bold></dark_gray>")
                .hoverEvent(HoverEvent.showText(Component.text("Desactiva la detección de Floodgate/Bedrock.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 2 off"));

        sender.sendMessage(yesBtn);
        sender.sendMessage(Component.empty());
        sender.sendMessage(noBtn);
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
    }

    // PASO 3: PROXY / REDIRECCIÓN A LOBBY
    private void sendStep3(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━ [ PASO 3 / 5: MODO DE RED ] ━━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(plugin.getLocaleManager().parse("<#E9D5FF>¿Este servidor es un Auth detrás de un Proxy (Velocity/Bungee) o es Standalone?</#E9D5FF>"));
        sender.sendMessage(Component.empty());

        Component proxyBtn = plugin.getLocaleManager().parse("  <gradient:#A855F7:#C084FC><bold>[ 🚀 Servidor Proxy (Redirigir a Lobby) ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Envía al jugador automáticamente al Lobby al autenticarse.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 3 proxy"));

        Component standaloneBtn = plugin.getLocaleManager().parse("  <gradient:#7C3AED:#9333EA><bold>[ 🏠 Servidor Individual (Standalone) ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("El jugador se queda jugando en este mismo servidor tras loguearse.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 3 standalone"));

        sender.sendMessage(proxyBtn);
        sender.sendMessage(Component.empty());
        sender.sendMessage(standaloneBtn);
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
    }

    // PASO 4: SUITE 2FA
    private void sendStep4(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━ [ PASO 4 / 5: SUITE 2FA ] ━━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(plugin.getLocaleManager().parse("<#E9D5FF>¿Deseas habilitar la suite 2FA (Google Auth con mapa QR, Discord, Telegram)?</#E9D5FF>"));
        sender.sendMessage(Component.empty());

        Component enable2fa = plugin.getLocaleManager().parse("  <gradient:#A855F7:#C084FC><bold>[ 🔐 Habilitar 2FA Suite (Recomendado) ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Permite /2fa setup con mapa QR en el juego y códigos de recuperación.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 4 on"));

        Component disable2fa = plugin.getLocaleManager().parse("  <dark_gray><bold>[ ✖ Desactivar 2FA ]</bold></dark_gray>")
                .hoverEvent(HoverEvent.showText(Component.text("Desactiva los módulos de doble factor.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 4 off"));

        sender.sendMessage(enable2fa);
        sender.sendMessage(Component.empty());
        sender.sendMessage(disable2fa);
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
    }

    // PASO 5: IDIOMA
    private void sendStep5(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━ [ PASO 5 / 5: IDIOMA ] ━━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(plugin.getLocaleManager().parse("<#E9D5FF>¿Cómo deseas manejar el idioma de los mensajes?</#E9D5FF>"));
        sender.sendMessage(Component.empty());

        Component autoBtn = plugin.getLocaleManager().parse("  <gradient:#A855F7:#C084FC><bold>[ 🌐 Auto-Detectar Idioma del Cliente ]</bold></gradient>")
                .hoverEvent(HoverEvent.showText(Component.text("Muestra los mensajes automáticamente según el idioma de Minecraft del jugador.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 5 auto"));

        Component esBtn = plugin.getLocaleManager().parse("  <#C084FC><bold>[ 🇪🇸 Fijar Español (Global) ]</bold></#C084FC>")
                .hoverEvent(HoverEvent.showText(Component.text("Todos los jugadores verán siempre los mensajes en Español.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 5 es"));

        Component enBtn = plugin.getLocaleManager().parse("  <#A855F7><bold>[ 🇺🇸 Fijar Inglés (Global) ]</bold></#A855F7>")
                .hoverEvent(HoverEvent.showText(Component.text("Todos los jugadores verán los mensajes en Inglés.", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.runCommand("/smartlogin wizard 5 en"));

        sender.sendMessage(autoBtn);
        sender.sendMessage(Component.empty());
        sender.sendMessage(esBtn);
        sender.sendMessage(Component.empty());
        sender.sendMessage(enBtn);
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
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
                    sender.sendMessage(plugin.getLocaleManager().parse("<light_purple>✔</light_purple> <gray>Almacenamiento configurado en: <#C084FC>MySQL / MariaDB</#C084FC></gray>"));
                } else {
                    dbConfig.set("type", "SQLITE");
                    plugin.getModularConfig().saveDatabase();
                    sender.sendMessage(plugin.getLocaleManager().parse("<light_purple>✔</light_purple> <gray>Almacenamiento configurado en: <#C084FC>SQLite (Local)</#C084FC></gray>"));
                }
                sendStep(sender, 2);
                break;

            case 2:
                boolean bedrockOn = "on".equalsIgnoreCase(choice);
                authConfig.set("bedrock.auto-login-enabled", bedrockOn);
                plugin.getModularConfig().saveAuth();
                sender.sendMessage(plugin.getLocaleManager().parse("<light_purple>✔</light_purple> <gray>Soporte Bedrock: <#C084FC>" + (bedrockOn ? "ACTIVADO" : "DESACTIVADO") + "</#C084FC></gray>"));
                sendStep(sender, 3);
                break;

            case 3:
                boolean proxyOn = "proxy".equalsIgnoreCase(choice);
                authConfig.set("proxy.send-to-lobby-on-login", proxyOn);
                plugin.getModularConfig().saveAuth();
                sender.sendMessage(plugin.getLocaleManager().parse("<light_purple>✔</light_purple> <gray>Modo de Red: <#C084FC>" + (proxyOn ? "Proxy (Redirección a Lobby)" : "Standalone (Individual)") + "</#C084FC></gray>"));
                sendStep(sender, 4);
                break;

            case 4:
                boolean twoFaOn = "on".equalsIgnoreCase(choice);
                totpConfig.set("enabled", twoFaOn);
                plugin.getModularConfig().saveTotp();
                sender.sendMessage(plugin.getLocaleManager().parse("<light_purple>✔</light_purple> <gray>Suite 2FA: <#C084FC>" + (twoFaOn ? "ACTIVADA" : "DESACTIVADA") + "</#C084FC></gray>"));
                sendStep(sender, 5);
                break;

            case 5:
                if ("auto".equalsIgnoreCase(choice)) {
                    config.set("general.auto-detect-client-language", true);
                    sender.sendMessage(plugin.getLocaleManager().parse("<light_purple>✔</light_purple> <gray>Detección automática de idioma: <#C084FC>ACTIVADA</#C084FC></gray>"));
                } else if ("es".equalsIgnoreCase(choice)) {
                    config.set("general.auto-detect-client-language", false);
                    config.set("general.default-language", "es");
                    sender.sendMessage(plugin.getLocaleManager().parse("<light_purple>✔</light_purple> <gray>Idioma fijado en: <#C084FC>ESPAÑOL</#C084FC></gray>"));
                } else if ("en".equalsIgnoreCase(choice)) {
                    config.set("general.auto-detect-client-language", false);
                    config.set("general.default-language", "en");
                    sender.sendMessage(plugin.getLocaleManager().parse("<light_purple>✔</light_purple> <gray>Idioma fijado en: <#C084FC>INGLÉS</#C084FC></gray>"));
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
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>╔══════════════════════════════════════════════════╗</bold></gradient>"));
        sender.sendMessage(plugin.getLocaleManager().parse("  <light_purple><bold>🎉 ¡CONFIGURACIÓN INICIAL COMPLETADA CON ÉXITO!</bold></light_purple>"));
        sender.sendMessage(plugin.getLocaleManager().parse("  <gray>Todos los ajustes han sido guardados correctamente.</gray>"));
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>👉 Panel de Control GUI: </#E9D5FF>")
                .append(Component.text("/smartlogin gui", NamedTextColor.LIGHT_PURPLE, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand("/smartlogin gui"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click para abrir el panel de control", NamedTextColor.GRAY)))));
        sender.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>👉 Ver todos los comandos: </#E9D5FF>")
                .append(Component.text("/smartlogin help", NamedTextColor.LIGHT_PURPLE, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand("/smartlogin help"))));
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>╚══════════════════════════════════════════════════╝</bold></gradient>"));

        if (sender instanceof Player player) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            adminCurrentStep.remove(player.getUniqueId());
        }
    }
}
