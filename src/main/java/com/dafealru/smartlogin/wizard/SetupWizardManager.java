package com.dafealru.smartlogin.wizard;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class SetupWizardManager {

    private final SmartLogin plugin;

    public SetupWizardManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public boolean isSetupCompleted() {
        return plugin.getModularConfig().getConfig().getBoolean("setup-completed", false);
    }

    public void sendSetupForm(CommandSender sender) {
        FileConfiguration config = plugin.getModularConfig().getConfig();
        FileConfiguration authConfig = plugin.getModularConfig().getAuthConfig();
        FileConfiguration totpConfig = plugin.getModularConfig().getTotpConfig();

        boolean bedrock = authConfig.getBoolean("bedrock.auto-login-enabled", true);
        boolean premium = authConfig.getBoolean("premium.auto-login-enabled", true);
        boolean twoFa = totpConfig.getBoolean("enabled", true);
        boolean staff2fa = totpConfig.getBoolean("staff-enforcement.enabled", true);
        boolean autoLang = config.getBoolean("general.auto-detect-client-language", true);
        String hashMode = authConfig.getString("hashing.mode", "FAST_PBKDF2");

        sender.sendMessage(Component.text("══════════════════════════════════════════════════", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("   ⚡ SmartLogin — Interactive Setup Wizard", NamedTextColor.YELLOW, TextDecoration.BOLD));
        sender.sendMessage(Component.text("Click any button to configure the server instantly:", NamedTextColor.GRAY));
        sender.sendMessage(Component.empty());

        // 1. Language Detection
        Component langBtn = Component.text(autoLang ? " [ ✔ AUTO-DETECT CLIENT ] " : " [ ✖ FIXED GLOBAL ] ",
                autoLang ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Click to toggle automatic client language detection!")))
                .clickEvent(ClickEvent.runCommand("/smartlogin toggle autolang"));
        sender.sendMessage(Component.text("1. Client Language Auto-Detect: ", NamedTextColor.WHITE).append(langBtn));

        // 2. Hash Mode
        boolean isFast = "FAST_PBKDF2".equalsIgnoreCase(hashMode);
        Component hashBtn = Component.text(isFast ? " [ ⚡ FAST (10k) ] " : " [ 🛡️ MILITARY (100k) ] ",
                NamedTextColor.AQUA, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Click to switch between Fast & Military-Grade PBKDF2 iterations!")))
                .clickEvent(ClickEvent.runCommand("/smartlogin toggle hashmode"));
        sender.sendMessage(Component.text("2. Password Hashing Speed: ", NamedTextColor.WHITE).append(hashBtn));

        // 3. Bedrock
        Component bedrockBtn = Component.text(bedrock ? " [ ✔ ON ] " : " [ ✖ OFF ] ",
                bedrock ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Toggle Bedrock / Geyser auto-login")))
                .clickEvent(ClickEvent.runCommand("/smartlogin toggle bedrock"));
        sender.sendMessage(Component.text("3. Bedrock Auto-Login: ", NamedTextColor.WHITE).append(bedrockBtn));

        // 4. Java Premium
        Component premiumBtn = Component.text(premium ? " [ ✔ ON ] " : " [ ✖ OFF ] ",
                premium ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Toggle Mojang Java Premium auto-login")))
                .clickEvent(ClickEvent.runCommand("/smartlogin toggle premium"));
        sender.sendMessage(Component.text("4. Java Premium Auto-Login: ", NamedTextColor.WHITE).append(premiumBtn));

        // 5. 2FA
        Component twoFaBtn = Component.text(twoFa ? " [ ✔ ON ] " : " [ ✖ OFF ] ",
                twoFa ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Toggle In-Game QR Map 2FA Engine")))
                .clickEvent(ClickEvent.runCommand("/smartlogin toggle 2fa"));
        sender.sendMessage(Component.text("5. Google Auth 2FA (QR Map): ", NamedTextColor.WHITE).append(twoFaBtn));

        // 6. Staff 2FA Enforcement
        Component staff2faBtn = Component.text(staff2fa ? " [ ✔ ON ] " : " [ ✖ OFF ] ",
                staff2fa ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Enforce mandatory 2FA for all Staff members")))
                .clickEvent(ClickEvent.runCommand("/smartlogin toggle staff2fa"));
        sender.sendMessage(Component.text("6. Staff Mandatory 2FA: ", NamedTextColor.WHITE).append(staff2faBtn));

        sender.sendMessage(Component.empty());
        Component finishBtn = Component.text("   [ 🚀 FINISH & COMPLETE SETUP ]   ", NamedTextColor.BLACK, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Click to save and mark initial setup as completed!")))
                .clickEvent(ClickEvent.runCommand("/smartlogin finishsetup"));
        sender.sendMessage(finishBtn);
        sender.sendMessage(Component.text("══════════════════════════════════════════════════", NamedTextColor.GOLD));
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

        sendSetupForm(sender);
    }

    public void finishSetup(CommandSender sender) {
        plugin.getModularConfig().getConfig().set("setup-completed", true);
        plugin.getModularConfig().saveConfig();
        sender.sendMessage(Component.text("✔ SmartLogin Setup Wizard completed successfully!", NamedTextColor.GREEN, TextDecoration.BOLD));
    }
}
