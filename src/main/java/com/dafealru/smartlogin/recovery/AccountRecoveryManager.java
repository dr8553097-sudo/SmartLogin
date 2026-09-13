package com.dafealru.smartlogin.recovery;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.PasswordHasher;
import com.dafealru.smartlogin.crypto.PasswordValidator;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Account Recovery Suite.
 * Handles self-service password reset with temporary 6-digit OTP codes
 * dispatched via linked Discord or Telegram.
 */
public class AccountRecoveryManager {

    private final SmartLogin plugin;
    private static final SecureRandom RANDOM = new SecureRandom();

    public record RecoverySession(String otp, long expiresAt) {}

    private final Map<UUID, RecoverySession> activeSessions = new ConcurrentHashMap<>();

    public AccountRecoveryManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public boolean requestRecovery(Player player) {
        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
        if (profile == null) return false;

        boolean hasTelegram = profile.getTelegramChatId() != null && !profile.getTelegramChatId().isEmpty();
        boolean hasDiscord = profile.getDiscordId() != null && !profile.getDiscordId().isEmpty();

        if (!hasTelegram && !hasDiscord) {
            return false;
        }

        String otp = String.format("%06d", RANDOM.nextInt(1000000));
        long expiresAt = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
        activeSessions.put(player.getUniqueId(), new RecoverySession(otp, expiresAt));

        String alertText = "🔑 *SmartLogin - Código de Recuperación de Cuenta*\n\n" +
                "Usuario: *" + player.getName() + "*\n" +
                "Código OTP: `" + otp + "`\n" +
                "Expira en: 5 minutos.\n\n" +
                "Para restablecer tu clave en el servidor escribe:\n" +
                "`/recover " + otp + " <nueva_contraseña>`\n\n" +
                "⚠️ _Si tú no solicitaste esto, ignora este mensaje y cambia tu clave._";

        if (hasTelegram) {
            plugin.getTelegramManager().sendDirectMessage(profile.getTelegramChatId(), alertText);
        }

        if (hasDiscord) {
            plugin.getDiscordManager().sendWebhookAlert(
                    "🔑 Código de Recuperación OTP",
                    "Usuario: **" + player.getName() + "**\nCódigo OTP enviado para recuperación de cuenta.",
                    0x9333EA
            );
        }

        return true;
    }

    public enum RecoveryResult {
        SUCCESS,
        INVALID_CODE,
        EXPIRED,
        WEAK_PASSWORD,
        NO_SESSION,
        DATABASE_ERROR
    }

    public RecoveryResult resetPassword(Player player, String code, String newPassword) {
        RecoverySession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            return RecoveryResult.NO_SESSION;
        }

        if (System.currentTimeMillis() > session.expiresAt()) {
            activeSessions.remove(player.getUniqueId());
            return RecoveryResult.EXPIRED;
        }

        if (!session.otp().equals(code.trim())) {
            return RecoveryResult.INVALID_CODE;
        }

        // Validate password strength
        var valResult = PasswordValidator.validate(plugin, player.getName(), newPassword);
        if (!valResult.valid()) {
            return RecoveryResult.WEAK_PASSWORD;
        }

        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return RecoveryResult.DATABASE_ERROR;
        }

        // Hash and update
        String salt = PasswordHasher.generateSalt();
        String newHash = PasswordHasher.hash(newPassword, salt);
        profile.setSalt(salt);
        profile.setPasswordHash(newHash);
        plugin.getDatabaseManager().saveProfile(profile);
        activeSessions.remove(player.getUniqueId());

        return RecoveryResult.SUCCESS;
    }
}
