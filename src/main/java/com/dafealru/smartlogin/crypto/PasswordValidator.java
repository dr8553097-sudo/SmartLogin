package com.dafealru.smartlogin.crypto;

import com.dafealru.smartlogin.SmartLogin;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Enterprise Password Policy & Strength Validator.
 * Prevents insecure passwords containing the player's username or matching top breached dictionaries.
 */
public class PasswordValidator {

    private static final Set<String> COMMON_PASSWORDS = new HashSet<>(Arrays.asList(
            "123456", "password", "12345678", "qwerty", "123456789", "12345", "1234", "111111",
            "1234567", "dragon", "welcome", "123123", "secret", "football", "iloveyou", "admin",
            "minecraft", "monkey", "charlie", "donald", "superman", "master", "shadow", "abc123",
            "killer", "solo", "roblox", "pokemon", "starwars", "batman", "freedom", "matrix",
            "trustno1", "letmein", "server", "sunshine", "princess", "cookie", "testing",
            "daniel", "alexander", "jordan", "michael", "joshua", "thomas", "william", "matthew",
            "chelsea", "arsenal", "liverpool", "barcelona", "realmadrid", "champion", "winner",
            "computer", "internet", "security", "spigot", "paper", "bukkit", "modpack", "creative",
            "survival", "hardcore", "diamond", "netherite", "emerald", "herobrine", "notched",
            "jeb_", "enderdragon", "witherskeleton", "creeper", "zombie", "skeleton", "spider"
    ));

    public record ValidationResult(boolean valid, String errorKey) {}

    public static ValidationResult validate(SmartLogin plugin, String username, String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "error-password-too-weak");
        }

        int minLen = 4;
        int maxLen = 32;
        if (plugin != null && plugin.getModularConfig() != null) {
            minLen = plugin.getModularConfig().getConfig().getInt("password-policy.min-length", 4);
            maxLen = plugin.getModularConfig().getConfig().getInt("password-policy.max-length", 32);
        }

        if (password.length() < minLen || password.length() > maxLen) {
            return new ValidationResult(false, "error-password-length");
        }

        String lowerPass = password.toLowerCase();
        String lowerUser = username != null ? username.toLowerCase() : "";

        // 1. Check if password is or contains the username (e.g. Pedro -> Pedro123, 123Pedro)
        if (!lowerUser.isEmpty()) {
            if (lowerPass.equals(lowerUser) || (lowerUser.length() >= 3 && lowerPass.contains(lowerUser))) {
                return new ValidationResult(false, "error-password-contains-name");
            }
        }

        // 2. Check against top common breached passwords
        if (COMMON_PASSWORDS.contains(lowerPass)) {
            return new ValidationResult(false, "error-password-too-weak");
        }

        // 3. Check disallowed passwords in config
        if (plugin != null && plugin.getModularConfig() != null) {
            var disallowed = plugin.getModularConfig().getConfig().getStringList("password-policy.disallowed-passwords");
            if (disallowed != null) {
                for (String dis : disallowed) {
                    if (lowerPass.equalsIgnoreCase(dis) || lowerPass.contains(dis.toLowerCase())) {
                        return new ValidationResult(false, "error-password-too-weak");
                    }
                }
            }
        }

        return new ValidationResult(true, null);
    }
}