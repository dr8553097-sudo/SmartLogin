package com.dafealru.smartlogin.telegram;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TelegramManager {

    private final SmartLogin plugin;
    private final Map<String, UUID> pendingLinkCodes = new HashMap<>();
    private final Map<UUID, String> pendingLinkCodesReverse = new HashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();

    public TelegramManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public boolean isBotConfigured() {
        boolean enabled = plugin.getModularConfig().getTelegramConfig().getBoolean("enabled", false);
        String token = plugin.getModularConfig().getTelegramConfig().getString("bot-token", "");
        return enabled && token != null && !token.isEmpty() && !token.contains("YOUR_TELEGRAM") && !token.contains("BOT_TOKEN");
    }

    public String generateLinkCode(Player player) {
        String existing = pendingLinkCodesReverse.get(player.getUniqueId());
        if (existing != null) return existing;

        String code = String.format("%06d", RANDOM.nextInt(1000000));
        pendingLinkCodes.put(code, player.getUniqueId());
        pendingLinkCodesReverse.put(player.getUniqueId(), code);
        return code;
    }

    public boolean completeLink(String code, String telegramChatId) {
        UUID uuid = pendingLinkCodes.remove(code);
        if (uuid == null) return false;
        pendingLinkCodesReverse.remove(uuid);

        PlayerProfile profile = plugin.getAuthManager().getProfile(uuid);
        if (profile != null) {
            profile.setTelegramChatId(telegramChatId);
            plugin.getDatabaseManager().saveProfile(profile);
            sendDirectMessage(telegramChatId, "✔ Tu cuenta de Minecraft *" + profile.getUsername() + "* ha sido vinculada exitosamente con SmartLogin.");
            return true;
        }
        return false;
    }

    public void sendDirectMessage(String chatId, String text) {
        String token = plugin.getModularConfig().getTelegramConfig().getString("bot-token", "");
        if (token == null || token.isEmpty() || token.contains("YOUR_TELEGRAM")) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String apiUrl = "https://api.telegram.org/bot" + token + "/sendMessage";
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                String postData = "chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8) +
                        "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8) +
                        "&parse_mode=Markdown";

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes(StandardCharsets.UTF_8));
                }
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {}
        });
    }

    public void sendSecurityAlert(String text) {
        boolean enabled = plugin.getModularConfig().getTelegramConfig().getBoolean("alerts.enabled", false);
        if (!enabled) return;

        String chatId = plugin.getModularConfig().getTelegramConfig().getString("alerts.chat-id", "");
        if (chatId != null && !chatId.isEmpty()) {
            sendDirectMessage(chatId, "🚨 *SMARTLOGIN SECURITY ALERT* 🚨\n\n" + text);
        }
    }
}
