package com.dafealru.smartlogin.proxy;

import com.dafealru.smartlogin.SmartLogin;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;

public class ProxyBridge {

    private final SmartLogin plugin;

    public ProxyBridge(SmartLogin plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "smartlogin:auth");
    }

    public void sendToLobby(Player player) {
        String targetServer = plugin.getModularConfig().getAuthConfig().getString("proxy.send-to-server-on-login", "");
        if (targetServer == null || targetServer.isEmpty()) return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(targetServer);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void broadcastAuthStatus(Player player, boolean authenticated) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(player.getName());
        out.writeBoolean(authenticated);
        player.sendPluginMessage(plugin, "smartlogin:auth", out.toByteArray());
    }
}
