package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LanguageCommand implements CommandExecutor, TabCompleter {

    private final SmartLogin plugin;
    private final List<String> supportedLanguages = Arrays.asList("es", "en", "pt", "fr", "de", "ru", "zh");

    private final Map<String, String> langNames = Map.of(
            "es", "Español",
            "en", "English",
            "pt", "Português",
            "fr", "Français",
            "de", "Deutsch",
            "ru", "Русский",
            "zh", "中文 (Chinese)"
    );

    public LanguageCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sendLanguageMenu(sender);
            return true;
        }

        String targetLang = args[0].toLowerCase();
        if (!supportedLanguages.contains(targetLang)) {
            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>✖ Idioma no válido / Invalid language. Usa: <#C084FC>/lang <es|en|pt|fr|de|ru|zh></#C084FC></#F5D0FE>"));
            return true;
        }

        if (sender instanceof Player player) {
            plugin.getLocaleManager().setPlayerLanguage(player.getUniqueId(), targetLang);
            String langName = langNames.getOrDefault(targetLang, targetLang.toUpperCase());
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ Idioma actualizado a: <#C084FC><bold>" + langName + " (" + targetLang.toUpperCase() + ")</bold></#C084FC></#E9D5FF>"));

            // Immediately refresh HUD prompts if player is not logged in yet
            if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                plugin.getAuthHudManager().stopHud(player);
                plugin.getDatabaseManager().loadProfile(player.getUniqueId()).thenAccept(profile -> {
                    boolean isRegister = profile == null || profile.getPasswordHash() == null;
                    plugin.getAuthHudManager().startHud(player, isRegister);
                });
            }
        } else {
            plugin.getLocaleManager().setDefaultLanguage(targetLang);
            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>✔ Server default language set to: <#C084FC><bold>" + targetLang.toUpperCase() + "</bold></#C084FC></#E9D5FF>"));
        }

        return true;
    }

    private void sendLanguageMenu(CommandSender sender) {
        sender.sendMessage(plugin.getLocaleManager().parse("\n<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ SMARTLOGIN — SELECT LANGUAGE ] ━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(plugin.getLocaleManager().parse(" <#E9D5FF>Haz clic en tu idioma / Click a language below:</#E9D5FF>\n"));

        Component buttons = Component.empty();
        for (String code : supportedLanguages) {
            String name = langNames.getOrDefault(code, code.toUpperCase());
            Component btn = plugin.getLocaleManager().parse("<gradient:#C084FC:#F5D0FE>[ " + name + " (" + code.toUpperCase() + ") ]</gradient> ")
                    .clickEvent(ClickEvent.runCommand("/lang " + code))
                    .hoverEvent(HoverEvent.showText(plugin.getLocaleManager().parse("<#E9D5FF>Click para seleccionar / Click to select <#C084FC>" + name + "</#C084FC></#E9D5FF>")));
            buttons = buttons.append(btn);
        }
        sender.sendMessage(buttons);
        sender.sendMessage(plugin.getLocaleManager().parse("\n<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>\n"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String l : supportedLanguages) {
                if (l.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(l);
                }
            }
            return list;
        }
        return List.of();
    }
}
