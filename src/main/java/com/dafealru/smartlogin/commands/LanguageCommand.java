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
            Component msg = plugin.getLocaleManager().getComponent("lang-changed", player);
            if (msg == null || msg.equals(Component.empty())) {
                msg = plugin.getLocaleManager().parse("<#E9D5FF>✔ Language updated to: <#C084FC><bold>" + langName + " (" + targetLang.toUpperCase() + ")</bold></#C084FC></#E9D5FF>");
            }
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> ").append(msg));

            // Refresh UI after language change
            plugin.refreshPlayerUI(player);
        } else {
            plugin.getLocaleManager().setDefaultLanguage(targetLang);
            // Reload configs and language dictionaries
            plugin.getModularConfig().loadAll();
            plugin.getLocaleManager().loadLanguages();
            // Refresh UI for all online players when changing global language
            plugin.getServer().getOnlinePlayers().forEach(plugin::refreshPlayerUI);

            Map<String, String> ph = Map.of("{lang}", targetLang.toUpperCase());
            Component changedMsg = plugin.getLocaleManager().getComponent("lang-server-changed", sender, ph);
            sender.sendMessage(plugin.getLocaleManager().parse(plugin.getLocaleManager().getRawMessage("prefix", targetLang)).append(changedMsg));
            sender.sendMessage(plugin.getLocaleManager().getComponent("admin-reloaded", sender));
        }

        return true;
    }

    private void sendLanguageMenu(CommandSender sender) {
        Player player = sender instanceof Player p ? p : null;
        sender.sendMessage(Component.empty());
        sender.sendMessage(plugin.getLocaleManager().getComponent("lang-menu-header", player));
        sender.sendMessage(plugin.getLocaleManager().getComponent("lang-menu-subtitle", player));
        sender.sendMessage(Component.empty());

        Component buttons = Component.empty();
        for (String code : supportedLanguages) {
            String name = langNames.getOrDefault(code, code.toUpperCase());
            Map<String, String> ph = Map.of("{name}", name);
            Component hover = plugin.getLocaleManager().getComponent("lang-menu-hover", player, ph);
            Component btn = plugin.getLocaleManager().parse("<gradient:#C084FC:#F5D0FE>[ " + name + " (" + code.toUpperCase() + ") ]</gradient> ")
                    .clickEvent(ClickEvent.runCommand("/lang " + code))
                    .hoverEvent(HoverEvent.showText(hover));
            buttons = buttons.append(btn);
        }
        sender.sendMessage(buttons);
        sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
        sender.sendMessage(Component.empty());
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
