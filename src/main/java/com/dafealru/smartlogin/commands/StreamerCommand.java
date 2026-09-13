package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StreamerCommand implements CommandExecutor {

    private final SmartLogin plugin;

    public StreamerCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLocaleManager().parse("<red>Este comando solo puede ser usado por jugadores en el servidor.</red>"));
            return true;
        }

        boolean enabled = plugin.getStreamerManager().toggleStreamerMode(player);
        if (enabled) {
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Streamer</bold></gradient> <dark_gray>»</dark_gray> <green>¡Modo Streamer <b>ACTIVADO</b>! Se ocultarán tus IPs en los menús y se bloquearán filtraciones de contraseñas en el chat.</green>"));
        } else {
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Streamer</bold></gradient> <dark_gray>»</dark_gray> <yellow>Modo Streamer <b>DESACTIVADO</b>.</yellow>"));
        }
        return true;
    }
}
