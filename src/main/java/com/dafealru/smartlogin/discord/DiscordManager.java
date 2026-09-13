package com.dafealru.smartlogin.discord;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise Discord 2FA & Link Suite.
 * Handles Discord bot REST interaction, DM login approvals, in-game /link codes, and security webhooks.
 */
public class DiscordManager {

    private final SmartLogin plugin;
    private final Map<String, LinkCodeEntry> pendingLinkCodes = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingLinkCodesReverse = new ConcurrentHashMap<>();
    private final Map<String, String> externalDiscordCodes = new ConcurrentHashMap<>(); // code -> discordId
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long CODE_EXPIRATION_MS = 10 * 60 * 1000L; // 10 minutes

    private record LinkCodeEntry(UUID uuid, long timestamp) {}

    public DiscordManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public boolean isBotConfigured() {
        boolean enabled = plugin.getModularConfig().getDiscordConfig().getBoolean("enabled", false) ||
                          plugin.getModularConfig().getDiscordConfig().getBoolean("bot.enabled", false);
        String token = plugin.getModularConfig().getDiscordConfig().getString("bot-token", "");
        if (token == null || token.isEmpty()) {
            token = plugin.getModularConfig().getDiscordConfig().getString("bot.token", "");
        }
        return enabled && token != null && !token.isEmpty() && !token.contains("YOUR_DISCORD") && !token.contains("TOKEN");
    }

    public String getBotToken() {
        String token = plugin.getModularConfig().getDiscordConfig().getString("bot-token", "");
        if (token == null || token.isEmpty() || token.contains("YOUR_DISCORD")) {
            token = plugin.getModularConfig().getDiscordConfig().getString("bot.token", "");
        }
        return token != null ? token.trim() : "";
    }

    public String generateLinkCode(Player player) {
        return generateLinkCode(player, false);
    }

    public String generateLinkCode(Player player, boolean forceNew) {
        UUID uuid = player.getUniqueId();
        if (!forceNew) {
            String existing = pendingLinkCodesReverse.get(uuid);
            if (existing != null) {
                LinkCodeEntry entry = pendingLinkCodes.get(existing);
                if (entry != null && System.currentTimeMillis() - entry.timestamp() < CODE_EXPIRATION_MS) {
                    return existing;
                }
            }
        }

        // Invalidate old code if any
        String oldCode = pendingLinkCodesReverse.remove(uuid);
        if (oldCode != null) {
            pendingLinkCodes.remove(oldCode);
        }

        String code = String.format("%06d", RANDOM.nextInt(1000000));
        pendingLinkCodes.put(code, new LinkCodeEntry(uuid, System.currentTimeMillis()));
        pendingLinkCodesReverse.put(uuid, code);
        return code;
    }

    public void registerExternalDiscordCode(String code, String discordUserId) {
        externalDiscordCodes.put(code.trim(), discordUserId.trim());
    }

    public boolean completeLinkWithCode(Player player, String code) {
        if (code == null) return false;
        String cleanCode = code.trim();

        // 1. Check if it matches an external code from Discord bot
        String discordId = externalDiscordCodes.remove(cleanCode);
        if (discordId != null) {
            PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
            if (profile != null) {
                profile.setDiscordId(discordId);
                plugin.getDatabaseManager().saveProfile(profile);

                // Clear pending codes
                String userCode = pendingLinkCodesReverse.remove(player.getUniqueId());
                if (userCode != null) pendingLinkCodes.remove(userCode);

                sendDirectMessage(discordId, "✔ ¡Tu cuenta de Minecraft **" + player.getName() + "** ha sido vinculada exitosamente con SmartLogin 2FA!");
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Discord</bold></gradient> <dark_gray>»</dark_gray> <green>✔ ¡Tu cuenta ha sido vinculada exitosamente con Discord 2FA!</green>"));
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                return true;
            }
        }

        // 2. Fallback check for player's own code
        LinkCodeEntry entry = pendingLinkCodes.get(cleanCode);
        if (entry != null && entry.uuid().equals(player.getUniqueId())) {
            pendingLinkCodes.remove(cleanCode);
            pendingLinkCodesReverse.remove(player.getUniqueId());
            return true;
        }

        return false;
    }

    public boolean completeLink(String code, String discordId) {
        LinkCodeEntry entry = pendingLinkCodes.remove(code);
        if (entry == null) return false;

        // Verify expiration
        if (System.currentTimeMillis() - entry.timestamp() > CODE_EXPIRATION_MS) {
            pendingLinkCodesReverse.remove(entry.uuid());
            return false;
        }

        pendingLinkCodesReverse.remove(entry.uuid());

        PlayerProfile profile = plugin.getAuthManager().getProfile(entry.uuid());
        if (profile != null) {
            profile.setDiscordId(discordId);
            plugin.getDatabaseManager().saveProfile(profile);

            // Send Discord DM
            sendDirectMessage(discordId, "✔ ¡Tu cuenta de Minecraft **" + profile.getUsername() + "** ha sido vinculada exitosamente con SmartLogin 2FA!");

            // Notify player in-game if online
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player p = Bukkit.getPlayer(entry.uuid());
                if (p != null && p.isOnline()) {
                    p.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Discord</bold></gradient> <dark_gray>»</dark_gray> <green>✔ ¡Tu cuenta ha sido vinculada exitosamente con Discord (<white>" + discordId + "</white>)!</green>"));
                    p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            });
            return true;
        }
        return false;
    }

    public boolean unlinkDiscord(Player player) {
        if (player == null) return false;
        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
        if (profile == null || profile.getDiscordId() == null || profile.getDiscordId().isEmpty()) {
            return false;
        }
        String oldDiscordId = profile.getDiscordId();
        profile.setDiscordId(null);
        plugin.getDatabaseManager().saveProfile(profile);

        // Clear any pending link codes
        String code = pendingLinkCodesReverse.remove(player.getUniqueId());
        if (code != null) pendingLinkCodes.remove(code);

        // In-game notification
        player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Discord</bold></gradient> <dark_gray>»</dark_gray> <yellow>✔ Tu cuenta de Minecraft ha sido desvinculada de Discord con éxito.</yellow>"));
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);

        // Send alert to Discord DM
        sendDirectMessage(oldDiscordId, "⚠️ Tu cuenta de Discord ha sido desvinculada de Minecraft (**" + player.getName() + "**).");
        return true;
    }

    public boolean unlinkDiscord(UUID uuid) {
        PlayerProfile profile = plugin.getAuthManager().getProfile(uuid);
        if (profile == null || profile.getDiscordId() == null || profile.getDiscordId().isEmpty()) {
            return false;
        }
        String oldDiscordId = profile.getDiscordId();
        profile.setDiscordId(null);
        plugin.getDatabaseManager().saveProfile(profile);

        String code = pendingLinkCodesReverse.remove(uuid);
        if (code != null) pendingLinkCodes.remove(code);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Discord</bold></gradient> <dark_gray>»</dark_gray> <yellow>✔ Tu cuenta de Minecraft ha sido desvinculada de Discord con éxito.</yellow>"));
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            }
        });

        sendDirectMessage(oldDiscordId, "⚠️ Tu cuenta de Discord ha sido desvinculada de Minecraft (**" + profile.getUsername() + "**).");
        return true;
    }

    public void sendDirectMessage(String discordUserId, String messageContent) {
        String token = getBotToken();
        if (token.isEmpty() || discordUserId == null || discordUserId.isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Step 1: Create DM Channel
                URL dmUrl = new URL("https://discord.com/api/v10/users/@me/channels");
                HttpURLConnection dmConn = (HttpURLConnection) dmUrl.openConnection();
                dmConn.setRequestMethod("POST");
                dmConn.setRequestProperty("Authorization", "Bot " + token);
                dmConn.setRequestProperty("Content-Type", "application/json");
                dmConn.setRequestProperty("User-Agent", "SmartLogin/1.0");
                dmConn.setDoOutput(true);

                String dmPayload = "{\"recipient_id\": \"" + discordUserId + "\"}";
                try (OutputStream os = dmConn.getOutputStream()) {
                    os.write(dmPayload.getBytes(StandardCharsets.UTF_8));
                }

                int dmStatus = dmConn.getResponseCode();
                if (dmStatus >= 200 && dmStatus < 300) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(dmConn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) response.append(line);
                    br.close();

                    // Parse channel ID
                    String json = response.toString();
                    int idIdx = json.indexOf("\"id\":");
                    if (idIdx != -1) {
                        int quote1 = json.indexOf("\"", idIdx + 5);
                        int quote2 = json.indexOf("\"", quote1 + 1);
                        if (quote1 != -1 && quote2 != -1) {
                            String channelId = json.substring(quote1 + 1, quote2);

                            // Step 2: Send Message to DM Channel
                            URL msgUrl = new URL("https://discord.com/api/v10/channels/" + channelId + "/messages");
                            HttpURLConnection msgConn = (HttpURLConnection) msgUrl.openConnection();
                            msgConn.setRequestMethod("POST");
                            msgConn.setRequestProperty("Authorization", "Bot " + token);
                            msgConn.setRequestProperty("Content-Type", "application/json");
                            msgConn.setRequestProperty("User-Agent", "SmartLogin/1.0");
                            msgConn.setDoOutput(true);

                            String msgPayload = "{\"content\": \"" + escapeJson(messageContent) + "\"}";
                            try (OutputStream os = msgConn.getOutputStream()) {
                                os.write(msgPayload.getBytes(StandardCharsets.UTF_8));
                            }
                            msgConn.getResponseCode();
                            msgConn.disconnect();
                        }
                    }
                }
                dmConn.disconnect();
            } catch (Exception ignored) {}
        });
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
