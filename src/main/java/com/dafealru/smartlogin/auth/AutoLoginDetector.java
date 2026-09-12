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
        if (!plugin.getModularConfig().getAuthConfig().getBoolean("bedrock.auto-login-enabled", true)) return false;
        
        // 1. Floodgate API Reflection
        if (isFloodgatePresent) {
            try {
                Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                Object apiInstance = floodgateApiClass.getMethod("getInstance").invoke(null);
                return (boolean) floodgateApiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(apiInstance, player.getUniqueId());
            } catch (Exception ignored) {}
        }
        
        // 2. Prefix fallback (e.g. '.' or '*' in Floodgate usernames)
        return player.getName().startsWith(".") || player.getName().startsWith("*");
    }

    public boolean isBedrockPlayer(Player player) {
        return isBedrock(player);
    }

    public boolean isJavaPremium(Player player) {
        if (!plugin.getModularConfig().getAuthConfig().getBoolean("premium.auto-login-enabled", true)) return false;
        // In modern 1.21 Paper servers with online-mode or Velocity/Bungee Forwarding
        return player.getPlayerProfile().hasProperty("textures");
    }

    public boolean isJavaPremiumPlayer(Player player) {
        return isJavaPremium(player);
    }
}
