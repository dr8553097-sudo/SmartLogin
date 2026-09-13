package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LinkCommand implements CommandExecutor, TabCompleter {

    private final SmartLogin plugin;

    public LinkCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Solo los jugadores pueden vincular cuentas.</#F5D0FE>"));
            return true;
        }

        if (!plugin.getDiscordManager().isBotConfigured()) {
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Discord</bold></gradient> <dark_gray>»</dark_gray> <red>El bot de Discord no está configurado en el servidor.</red>"));
            return true;
        }

        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());

        if (args.length >= 1) {
            String sub = args[0].toLowerCase();
            if (sub.equals("unlink") || sub.equals("desvincular") || sub.equals("remove") || sub.equals("delete")) {
                if (profile == null || profile.getDiscordId() == null || profile.getDiscordId().isEmpty()) {
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Discord</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Tu cuenta no está vinculada a ningún usuario de Discord.</#F5D0FE>"));
                    return true;
                }
                plugin.getDiscordManager().unlinkDiscord(player);
                return true;
            }

            if (sub.equals("new") || sub.equals("reset") || sub.equals("nuevo")) {
                String newCode = plugin.getDiscordManager().generateLinkCode(player, true);
                player.sendMessage(Component.empty());
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ 🤖 NUEVO CÓDIGO DISCORD ] ━━━━━━━━━━━━</bold></gradient>"));
                player.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>Nuevo código de vinculación:</#E9D5FF> <gradient:#C084FC:#F5D0FE><bold>[ " + newCode + " ]</bold></gradient>"));
                player.sendMessage(plugin.getLocaleManager().parse("  <gray>Envíalo al bot de Discord por MD o usa:</gray> <#C084FC><bold>/link " + newCode + "</bold></#C084FC>"));
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
                player.sendMessage(Component.empty());
                return true;
            }

            if (sub.equals("status") || sub.equals("info") || sub.equals("estado")) {
                boolean linked = profile != null && profile.getDiscordId() != null && !profile.getDiscordId().isEmpty();
                player.sendMessage(Component.empty());
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ 🤖 ESTADO DISCORD 2FA ] ━━━━━━━━━━━━</bold></gradient>"));
                player.sendMessage(plugin.getLocaleManager().parse("  <gray>Estado:</gray> " + (linked ? "<green><bold>✔ VINCULADO</bold></green>" : "<red><bold>✖ NO VINCULADO</bold></red>")));
                if (linked) {
                    player.sendMessage(plugin.getLocaleManager().parse("  <gray>Discord ID:</gray> <#818CF8>" + profile.getDiscordId() + "</#818CF8>"));
                    player.sendMessage(plugin.getLocaleManager().parse("  <gray>Para desvincular escribe:</gray> <#C084FC><bold>/link unlink</bold></#C084FC>"));
                } else {
                    player.sendMessage(plugin.getLocaleManager().parse("  <gray>Para vincular escribe:</gray> <#C084FC><bold>/link</bold></#C084FC>"));
                }
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
                player.sendMessage(Component.empty());
                return true;
            }

            // Otherwise attempt code validation
            String inputCode = args[0].trim();
            boolean linked = plugin.getDiscordManager().completeLinkWithCode(player, inputCode);
            if (!linked) {
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Discord</bold></gradient> <dark_gray>»</dark_gray> <red>Código de vinculación inválido o no reconocido. Usa /link para generar tu propio código o revisa el comando en Discord.</red>"));
            }
            return true;
        }

        // No arguments: check if already linked
        if (profile != null && profile.getDiscordId() != null && !profile.getDiscordId().isEmpty()) {
            player.sendMessage(Component.empty());
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ 🤖 DISCORD 2FA YA ACTIVO ] ━━━━━━━━━━━━</bold></gradient>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>Tu cuenta ya se encuentra vinculada a Discord ID:</#E9D5FF> <#818CF8><bold>" + profile.getDiscordId() + "</bold></#818CF8>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <gray>• Para desvincular escribe:</gray> <#C084FC><bold>/link unlink</bold></#C084FC>"));
            player.sendMessage(plugin.getLocaleManager().parse("  <gray>• Para generar un nuevo código escribe:</gray> <#C084FC><bold>/link new</bold></#C084FC>"));
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
            player.sendMessage(Component.empty());
            return true;
        }

        String code = plugin.getDiscordManager().generateLinkCode(player, false);
        player.sendMessage(Component.empty());
        player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ 🤖 SMARTLOGIN — VINCULACIÓN DISCORD ] ━━━━━━━━━━━━</bold></gradient>"));
        player.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>Tu código de vinculación:</#E9D5FF> <gradient:#C084FC:#F5D0FE><bold>[ " + code + " ]</bold></gradient>"));
        player.sendMessage(plugin.getLocaleManager().parse("  <gray>Envíale este código al bot de Discord por MD o usa:</gray> <#C084FC><bold>/link " + code + "</bold></#C084FC>"));
        player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
        player.sendMessage(Component.empty());
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("unlink", "new", "status");
            return options.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}
