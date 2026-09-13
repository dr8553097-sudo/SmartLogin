package com.dafealru.smartlogin.auth;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.entity.Player;
import java.net.InetSocketAddress;

public class SessionShield {

    private final SmartLogin plugin;

    public SessionShield(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public boolean isSessionValid(Player player, PlayerProfile profile) {
        if (profile == null || profile.getPasswordHash() == null) return false;
        
        boolean enabled = plugin.getModularConfig().getAuthConfig().getBoolean("session-shield.enabled", true);
        if (!enabled) return false;

        long timeoutMs = plugin.getModularConfig().getAuthConfig().getLong("session-shield.session-duration-minutes", 60) * 60L * 1000L;
        if (timeoutMs <= 0) return false;

        String currentIp = getPlayerIp(player);
        long now = System.currentTimeMillis();

        return currentIp.equals(profile.getLastIp()) && (now - profile.getLastLoginTimestamp() < timeoutMs);
    }

    public boolean isSessionValid(java.util.UUID uuid, String ip) {
        PlayerProfile profile = plugin.getAuthManager().getProfile(uuid);
        if (profile == null || profile.getPasswordHash() == null) return false;

        boolean enabled = plugin.getModularConfig().getAuthConfig().getBoolean("session-shield.enabled", true);
        if (!enabled) return false;

        long timeoutMs = plugin.getModularConfig().getAuthConfig().getLong("session-shield.session-duration-minutes", 60) * 60L * 1000L;
        if (timeoutMs <= 0) return false;

        long now = System.currentTimeMillis();
        return ip != null && ip.equals(profile.getLastIp()) && (now - profile.getLastLoginTimestamp() < timeoutMs);
    }

    public String getPlayerIp(Player player) {
        InetSocketAddress addr = player.getAddress();
        if (addr == null) return "127.0.0.1";
        return addr.getAddress().getHostAddress();
    }

    public void invalidateSession(java.util.UUID uuid) {
        PlayerProfile cached = plugin.getAuthManager().getProfile(uuid);
        if (cached != null) {
            cached.setLastLoginTimestamp(0);
            plugin.getDatabaseManager().saveProfile(cached);
            return;
        }
        plugin.getDatabaseManager().loadProfile(uuid).thenAccept(profile -> {
            if (profile != null) {
                profile.setLastLoginTimestamp(0);
                plugin.getDatabaseManager().saveProfile(profile);
            }
        });
    }

    public void clearAllSessions() {
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getAuthManager().getProfile(p.getUniqueId());
            if (profile != null) {
                profile.setLastLoginTimestamp(0);
                plugin.getDatabaseManager().saveProfile(profile);
            }
        }
    }
}
