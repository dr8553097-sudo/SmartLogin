package com.dafealru.smartlogin.proxy;

import com.dafealru.smartlogin.SmartLogin;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Enterprise BungeeCord & Velocity Proxy Engine.
 * Features:
 *  - Cross-Proxy Auth Synchronization
 *  - Lobby/Hub Automatic Redirection (Connect / ConnectOther)
 *  - Anti-Proxy Bypass Guard (Blocks direct unauthorized connections to backend port)
 *  - Modern Velocity & BungeeCord Forwarding Compatibility
 */
public class ProxyBridge implements PluginMessageListener {

    private final SmartLogin plugin;
    public static final String BUNGEE_CHANNEL = "BungeeCord";
    public static final String SMARTLOGIN_CHANNEL = "smartlogin:main";
    public static final String SMARTLOGIN_AUTH_CHANNEL = "smartlogin:auth";

    public ProxyBridge(SmartLogin plugin) {
        this.plugin = plugin;
        try {
            // Outgoing channels
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, SMARTLOGIN_CHANNEL);
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, SMARTLOGIN_AUTH_CHANNEL);

            // Incoming channels
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, SMARTLOGIN_CHANNEL, this);
        } catch (Exception ignored) {}
    }

    public boolean isProxyEnabled() {
        return plugin.getModularConfig().getAuthConfig().getBoolean("proxy.enabled", false);
    }

    /**
     * Checks if a direct backend port connection should be blocked when proxy mode is enabled.
     */
    public boolean isDirectConnectionBlocked(Player player) {
        if (!isProxyEnabled()) return false;
        boolean blockDirect = plugin.getModularConfig().getAuthConfig().getBoolean("proxy.block-direct-connections", true);
        if (!blockDirect) return false;

        String ip = player.getAddress().getAddress().getHostAddress();
        List<String> allowedIps = plugin.getModularConfig().getAuthConfig().getStringList("proxy.allowed-proxy-ips");
        if (allowedIps.isEmpty()) {
            allowedIps = List.of("127.0.0.1", "0:0:0:0:0:0:0:1", "localhost");
        }

        return !allowedIps.contains(ip);
    }

    public void sendToLobby(Player player) {
        String targetServer = plugin.getModularConfig().getAuthConfig().getString("proxy.send-to-server-on-login", "");
        if (targetServer == null || targetServer.isEmpty() || !isProxyEnabled()) return;
        sendToServer(player, targetServer);
    }

    public void sendToServer(Player player, String serverName) {
        if (player == null || serverName == null || serverName.isEmpty()) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            try {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(serverName);
                player.sendPluginMessage(plugin, BUNGEE_CHANNEL, out.toByteArray());
            } catch (Exception ignored) {}
        }, 5L);
    }

    public void broadcastAuthStatus(Player player, boolean authenticated) {
        if (!isProxyEnabled()) return;
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("AuthSync");
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(player.getName());
            out.writeBoolean(authenticated);
            player.sendPluginMessage(plugin, SMARTLOGIN_AUTH_CHANNEL, out.toByteArray());
        } catch (Exception ignored) {}
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(BUNGEE_CHANNEL) && !channel.equals(SMARTLOGIN_CHANNEL)) return;

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subChannel = in.readUTF();

            if ("AuthSync".equalsIgnoreCase(subChannel)) {
                String uuidStr = in.readUTF();
                String name = in.readUTF();
                boolean auth = in.readBoolean();
                Player target = Bukkit.getPlayer(name);
                if (target != null && auth) {
                    plugin.getAuthManager().setAuthenticated(target.getUniqueId(), true);
                }
            }
        } catch (Exception ignored) {}
    }
}
