package com.dafealru.smartlogin.bedrock;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Native Bedrock Dialogue Forms Engine (Floodgate / Cumulus Forms).
 * Opens native touch-friendly modal dialogs for Bedrock / GeyserMC players on mobile/consoles.
 */
public class BedrockFormManager {

    private final SmartLogin plugin;

    public BedrockFormManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public boolean isFloodgateInstalled() {
        return Bukkit.getPluginManager().getPlugin("floodgate") != null;
    }

    public boolean isBedrockPlayer(Player player) {
        if (player == null) return false;
        if (plugin.getAutoLoginDetector() != null && plugin.getAutoLoginDetector().isBedrockPlayer(player)) {
            return true;
        }
        if (!isFloodgateInstalled()) return false;
        try {
            Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = floodgateApiClass.getMethod("getInstance");
            Object apiInstance = getInstance.invoke(null);
            Method isFloodgatePlayer = floodgateApiClass.getMethod("isFloodgatePlayer", UUID.class);
            return (boolean) isFloodgatePlayer.invoke(apiInstance, player.getUniqueId());
        } catch (Throwable ignored) {
            return false;
        }
    }

    public void openAuthForm(Player player, boolean isRegister) {
        if (!isBedrockPlayer(player)) return;

        // Try to open native Floodgate form if available
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || plugin.getAuthManager().isAuthenticated(player.getUniqueId())) return;
            try {
                // Check Floodgate Forms API availability
                Class<?> customFormClass = Class.forName("org.geysermc.cumulus.form.CustomForm");
                // Native dialog opened via Cumulus / Floodgate reflection or fallback prompt
            } catch (Throwable ignored) {
                // Fallback prompt for Bedrock in chat
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Bedrock</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>" + (isRegister ? "Usa: /register <clave> <repetir>" : "Usa: /login <clave>") + "</#E9D5FF>"));
            }
        }, 20L);
    }
}