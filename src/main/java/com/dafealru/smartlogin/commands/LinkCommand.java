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

public class LinkCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public LinkCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can link Discord accounts.");
            return true;
        }

        String code = plugin.getDiscordManager().generateLinkCode(player);
        player.sendMessage(Component.text("════════════ 🤖 DISCORD LINKING ════════════", NamedTextColor.AQUA, TextDecoration.BOLD));
        player.sendMessage(Component.text("Your 6-digit Discord verification code is: ", NamedTextColor.WHITE)
                .append(Component.text(code, NamedTextColor.GOLD, TextDecoration.BOLD)));
        player.sendMessage(Component.text("Send this code to the Discord Bot to verify your account!", NamedTextColor.GRAY));
        player.sendMessage(Component.text("═══════════════════════════════════════════", NamedTextColor.AQUA));
        return true;
    }
}
