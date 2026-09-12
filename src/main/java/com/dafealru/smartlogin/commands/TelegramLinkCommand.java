package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TelegramLinkCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public TelegramLinkCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>Solo los jugadores pueden vincular cuentas.</#F5D0FE>"));
            return true;
        }

        if (!plugin.getTelegramManager().isBotConfigured()) {
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#F5D0FE>No hay ningún bot de Telegram configurado o vinculado en el servidor.</#F5D0FE>"));
            return true;
        }

        String code = plugin.getTelegramManager().generateLinkCode(player);
        player.sendMessage(Component.empty());
        player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━ [ SMARTLOGIN — VINCULACIÓN TELEGRAM ] ━━━━━━━━━━━━</bold></gradient>"));
        player.sendMessage(plugin.getLocaleManager().parse("  <#E9D5FF>Tu código de verificación de 6 dígitos es: </#E9D5FF><#C084FC><bold>" + code + "</bold></#C084FC>"));
        player.sendMessage(plugin.getLocaleManager().parse("  <gray>Envía <#C084FC>/link " + code + "</#C084FC> al bot de Telegram del servidor para vincular tu cuenta.</gray>"));
        player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</bold></gradient>"));
        player.sendMessage(Component.empty());
        return true;
    }
}
