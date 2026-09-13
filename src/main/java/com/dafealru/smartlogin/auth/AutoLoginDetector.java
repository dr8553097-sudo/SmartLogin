package com.dafealru.smartlogin.auth;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.UUID;

/**
 * Detects Bedrock (Geyser/Floodgate) and Java Premium Mojang Accounts.
 */
public class AutoLoginDetector {

    private final SmartLogin plugin;
    private final boolean isFloodgatePresent;

    public AutoLoginDetector(SmartLogin plugin) {
        this.plugin = plugin;
        this.isFloodgatePresent = Bukkit.getPluginManager().getPlugin("floodgate") != null;
    }

    public boolean isBedrock(Player player) {
        if (player == null) return false;
        if (!plugin.getModularConfig().getAuthConfig().getBoolean("bedrock.auto-login-enabled", true)) return false;
        
        boolean strictFloodgate = plugin.getModularConfig().getAuthConfig().getBoolean("bedrock.strict-floodgate-validation", true);

        // 1. Floodgate API Reflection (Primary & Most Secure)
        if (isFloodgatePresent) {
            try {
                Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                Object apiInstance = floodgateApiClass.getMethod("getInstance").invoke(null);
                boolean isFloodgate = (boolean) floodgateApiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(apiInstance, player.getUniqueId());
                if (isFloodgate) return true;

                // If Floodgate is active and says FALSE, reject any dot-prefix impostors trying to spoof Bedrock!
                if (strictFloodgate) {
                    return false;
                }
            } catch (Exception ignored) {}
        }
        
        // 2. Geyser Floodgate UUID Format Check (XUID high bits zero or special Bedrock UUIDs)
        UUID uuid = player.getUniqueId();
        if (uuid.getMostSignificantBits() == 0L || uuid.version() == 0) {
            return true;
        }

        // 3. Prefix fallback for standalone Bungee/Velocity setups
        var allowedPrefixes = plugin.getModularConfig().getAuthConfig().getStringList("bedrock.allowed-prefixes");
        if (allowedPrefixes.isEmpty()) {
            allowedPrefixes = java.util.List.of(".", "*", "_");
        }
        for (String p : allowedPrefixes) {
            if (player.getName().startsWith(p)) return true;
        }

        return false;
    }

    public boolean isBedrockPlayer(Player player) {
        return isBedrock(player);
    }

    public boolean isJavaPremium(Player player) {
        if (player == null) return false;
        if (!plugin.getModularConfig().getAuthConfig().getBoolean("premium.auto-login-enabled", true)) return false;

        // 1. FastLogin Hook Reflection (if present on server)
        if (Bukkit.getPluginManager().getPlugin("FastLogin") != null) {
            try {
                Class<?> fastLoginClass = Class.forName("com.github.games647.fastlogin.bukkit.FastLoginBukkit");
                Object flInstance = Bukkit.getPluginManager().getPlugin("FastLogin");
                if (flInstance != null) {
                    Object status = fastLoginClass.getMethod("getStatus", UUID.class).invoke(flInstance, player.getUniqueId());
                    if (status != null && "PREMIUM".equalsIgnoreCase(status.toString())) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. Paper 1.21 Native Signed Texture / Mojang Cryptographic Signature Check
        boolean strictSignature = plugin.getModularConfig().getAuthConfig().getBoolean("premium.strict-texture-signature-check", true);
        try {
            com.destroystokyo.paper.profile.PlayerProfile paperProfile = player.getPlayerProfile();
            for (com.destroystokyo.paper.profile.ProfileProperty prop : paperProfile.getProperties()) {
                if ("textures".equalsIgnoreCase(prop.getName())) {
                    if (strictSignature) {
                        return prop.getSignature() != null && !prop.getSignature().isEmpty() && prop.getValue() != null && !prop.getValue().isEmpty();
                    } else {
                        return prop.getValue() != null && !prop.getValue().isEmpty();
                    }
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    public boolean isJavaPremiumPlayer(Player player) {
        return isJavaPremium(player);
    }
}
