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
        
        long timeoutMs = plugin.getConfigManager().getSessionTimeoutMinutes() * 60L * 1000L;
        if (timeoutMs <= 0) return false;

        String currentIp = getPlayerIp(player);
        long now = System.currentTimeMillis();

        return currentIp.equals(profile.getLastIp()) && (now - profile.getLastLoginTimestamp() < timeoutMs);
    }

    public String getPlayerIp(Player player) {
        InetSocketAddress addr = player.getAddress();
        if (addr == null) return "127.0.0.1";
        return addr.getAddress().getHostAddress();
    }
}
