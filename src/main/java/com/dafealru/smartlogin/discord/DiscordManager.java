package com.dafealru.smartlogin.discord;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DiscordManager {

    private final SmartLogin plugin;
    private final Map<String, UUID> pendingLinkCodes = new HashMap<>();
    private final Map<UUID, String> pendingLinkCodesReverse = new HashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();

    public DiscordManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public String generateLinkCode(Player player) {
        String existing = pendingLinkCodesReverse.get(player.getUniqueId());
        if (existing != null) return existing;

        String code = String.format("%06d", RANDOM.nextInt(1000000));
        pendingLinkCodes.put(code, player.getUniqueId());
        pendingLinkCodesReverse.put(player.getUniqueId(), code);
        return code;
    }

    public boolean completeLink(String code, String discordId) {
        UUID uuid = pendingLinkCodes.remove(code);
        if (uuid == null) return false;
        pendingLinkCodesReverse.remove(uuid);

        PlayerProfile profile = plugin.getAuthManager().getProfile(uuid);
        if (profile != null) {
            profile.setDiscordId(discordId);
            plugin.getDatabaseManager().saveProfile(profile);
            return true;
        }
        return false;
    }

    public void sendWebhookAlert(String title, String description, int colorHex) {
        boolean enabled = plugin.getModularConfig().getDiscordConfig().getBoolean("webhooks.enabled", false);
        if (!enabled) return;

        String webhookUrl = plugin.getModularConfig().getDiscordConfig().getString("webhooks.staff-security-logs-url", "");
        if (webhookUrl == null || webhookUrl.isEmpty() || !webhookUrl.startsWith("http")) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "SmartLogin-Webhook");
                conn.setDoOutput(true);

                String json = "{\"embeds\": [{" +
                        "\"title\": \"" + escapeJson(title) + "\"," +
                        "\"description\": \"" + escapeJson(description) + "\"," +
                        "\"color\": " + colorHex + "," +
                        "\"footer\": {\"text\": \"SmartLogin Security Suite 2026\"}," +
                        "\"timestamp\": \"" + java.time.Instant.now().toString() + "\"" +
                        "}]}";

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {}
        });
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
