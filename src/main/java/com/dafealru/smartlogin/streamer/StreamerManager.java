package com.dafealru.smartlogin.streamer;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Streamer Protection Suite.
 * Protects streamers and content creators from accidental password leaks,
 * masks sensitive IP/Discord data in GUIs, and blocks leaked credentials in chat.
 */
public class StreamerManager {

    private final SmartLogin plugin;
    private final Set<UUID> streamerPlayers = ConcurrentHashMap.newKeySet();

    public StreamerManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public boolean isStreamerMode(UUID uuid) {
        return streamerPlayers.contains(uuid);
    }

    public boolean isStreamerMode(Player player) {
        return player != null && streamerPlayers.contains(player.getUniqueId());
    }

    public boolean toggleStreamerMode(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        if (streamerPlayers.contains(uuid)) {
            streamerPlayers.remove(uuid);
            return false;
        } else {
            streamerPlayers.add(uuid);
            return true;
        }
    }

    public String maskIp(String ip, boolean forceMask) {
        if (ip == null || ip.isEmpty()) return "***.***.***.***";
        if (forceMask || !plugin.getModularConfig().getConfig().getBoolean("streamer-protection.mask-ips", true)) return "***.***.***.***";
        return ip.replaceAll("\\.\\d+$", ".***");
    }

    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "u***@email.com";
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + domain;
        }
        return name.substring(0, 2) + "***@" + domain;
    }

    public String maskId(String id) {
        if (id == null || id.isEmpty()) return "N/A";
        if (id.length() <= 4) return "****";
        return id.substring(0, 3) + "****" + id.substring(id.length() - 2);
    }

    /**
     * Checks if a public chat message looks like an accidentally sent password or auth command.
     */
    public boolean isSuspiciousChat(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        String raw = message.trim();
        String lower = raw.toLowerCase();

        // 1. Missing slash before auth commands
        if (lower.startsWith("login ") || lower.startsWith("l ") ||
            lower.startsWith("register ") || lower.startsWith("reg ") ||
            lower.startsWith("changepassword ") || lower.startsWith("changepw ") || lower.startsWith("cpw ") ||
            lower.startsWith("2fa ") || lower.startsWith("email ") ||
            lower.startsWith("link ") || lower.startsWith("tlink ") ||
            lower.startsWith("pinpad ") || lower.startsWith("anvil ") ||
            lower.startsWith("loguin ") || lower.startsWith("registre ") ||
            lower.startsWith("clave ") || lower.startsWith("contra ")) {
            return true;
        }

        // 2. Auth command starting with slash but mistakenly sent
        if (lower.startsWith("/login") || lower.startsWith("/register") || lower.startsWith("/changepassword") ||
            lower.startsWith("/l ") || lower.startsWith("/reg ") || lower.startsWith("/2fa ")) {
            return true;
        }

        return false;
    }
}
