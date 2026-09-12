package com.dafealru.smartlogin.wizard;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Interactive Clickable Setup Wizard for SmartLogin.
 */
public class SetupWizardManager {

    private final SmartLogin plugin;

    public SetupWizardManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public void sendWizard(CommandSender sender) {
        var lm = plugin.getLocaleManager();
        var cfg = plugin.getConfigManager();

        sender.sendMessage(lm.parse("<gradient:#9d4edd:#00f0ff><bold>══════════════════════════════════════════════════</bold></gradient>"));
        sender.sendMessage(lm.parse("<bold><gradient:#c77dff:#00f0ff>   ⚡ SmartLogin — Interactive Setup Wizard</gradient></bold>"));
        sender.sendMessage(lm.parse("<gray>Click on any button below to toggle settings instantly:</gray>\n"));

        // 1. Bedrock Auto-Login Toggle
        String bedrockStatus = cfg.isBedrockAutoLogin() ? "<green>[ ✔ ON ]</green>" : "<red>[ ✖ OFF ]</red>";
        Component bedrockBtn = lm.parse("<yellow>1. Bedrock Auto-Login:</yellow> " + bedrockStatus)
                .clickEvent(ClickEvent.runCommand("/smartlogin toggle bedrock"))
                .hoverEvent(HoverEvent.showText(lm.parse("<gray>Click to toggle Bedrock (Xbox) Auto-Login</gray>")));
        sender.sendMessage(bedrockBtn);

        // 2. Premium Auto-Login Toggle
        String premiumStatus = cfg.isPremiumAutoLogin() ? "<green>[ ✔ ON ]</green>" : "<red>[ ✖ OFF ]</red>";
        Component premiumBtn = lm.parse("<yellow>2. Java Premium Auto-Login:</yellow> " + premiumStatus)
                .clickEvent(ClickEvent.runCommand("/smartlogin toggle premium"))
                .hoverEvent(HoverEvent.showText(lm.parse("<gray>Click to toggle Java Mojang Premium Auto-Login</gray>")));
        sender.sendMessage(premiumBtn);

        // 3. 2FA TOTP Toggle
        String totpStatus = cfg.isTotpEnabled() ? "<green>[ ✔ ON ]</green>" : "<red>[ ✖ OFF ]</red>";
        Component totpBtn = lm.parse("<yellow>3. Google Auth 2FA (QR Map):</yellow> " + totpStatus)
                .clickEvent(ClickEvent.runCommand("/smartlogin toggle totp"))
                .hoverEvent(HoverEvent.showText(lm.parse("<gray>Click to toggle In-Game QR Code Map 2FA</gray>")));
        sender.sendMessage(totpBtn);

        // 4. Blindness & Lockdown
        String blindStatus = cfg.isBlindnessEnabled() ? "<green>[ ✔ ON ]</green>" : "<red>[ ✖ OFF ]</red>";
        Component blindBtn = lm.parse("<yellow>4. Blindness & Freeze Lockdown:</yellow> " + blindStatus)
                .clickEvent(ClickEvent.runCommand("/smartlogin toggle blindness"))
                .hoverEvent(HoverEvent.showText(lm.parse("<gray>Click to toggle visual blindness before login</gray>")));
        sender.sendMessage(blindBtn);

        sender.sendMessage(lm.parse("\n<gradient:#9d4edd:#00f0ff><bold>══════════════════════════════════════════════════</bold></gradient>"));
    }
}
