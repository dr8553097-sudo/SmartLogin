package com.dafealru.smartlogin.auth;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {

    public enum AuthState {
        UNAUTHENTICATED,
        AWAITING_2FA,
        LOGGED_IN
    }

    private final SmartLogin plugin;
    private final Map<UUID, AuthState> playerStates = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> loginAttempts = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerProfile> cachedProfiles = new ConcurrentHashMap<>();

    public AuthManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public boolean isAuthenticated(UUID uuid) {
        return playerStates.getOrDefault(uuid, AuthState.UNAUTHENTICATED) == AuthState.LOGGED_IN;
    }

    public void setAuthenticated(UUID uuid, boolean auth) {
        playerStates.put(uuid, auth ? AuthState.LOGGED_IN : AuthState.UNAUTHENTICATED);
    }

    public void removeAuthenticated(UUID uuid) {
        uncache(uuid);
    }

    public AuthState getState(UUID uuid) {
        return playerStates.getOrDefault(uuid, AuthState.UNAUTHENTICATED);
    }

    public void setState(UUID uuid, AuthState state) {
        playerStates.put(uuid, state);
    }

    public void cacheProfile(UUID uuid, PlayerProfile profile) {
        if (profile != null) {
            cachedProfiles.put(uuid, profile);
        }
    }

    public void cacheProfile(PlayerProfile profile) {
        if (profile != null) {
            cachedProfiles.put(profile.getUuid(), profile);
        }
    }

    public PlayerProfile getProfile(UUID uuid) {
        return cachedProfiles.get(uuid);
    }

    public PlayerProfile getCachedProfile(UUID uuid) {
        return cachedProfiles.get(uuid);
    }

    public void uncache(UUID uuid) {
        playerStates.remove(uuid);
        loginAttempts.remove(uuid);
        cachedProfiles.remove(uuid);
    }

    public int incrementAttempts(UUID uuid) {
        int attempts = loginAttempts.getOrDefault(uuid, 0) + 1;
        loginAttempts.put(uuid, attempts);
        return attempts;
    }

    public void resetAttempts(UUID uuid) {
        loginAttempts.remove(uuid);
    }

    public void completeAuthentication(Player player, String messageKey) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> completeAuthentication(player, messageKey));
            return;
        }

        UUID uuid = player.getUniqueId();
        setAuthenticated(uuid, true);
        resetAttempts(uuid);

        // 1. Stop HUD BossBar and wipe Actionbar
        plugin.getAuthHudManager().stopHud(player);
        player.sendActionBar(net.kyori.adventure.text.Component.empty());

        // 2. Play Level Up & Success Sound
        plugin.getAuthHudManager().playSuccessSound(player);

        // 3. Remove All Active Potion Effects
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.DARKNESS);

        // 4. Restore flight settings
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        // 5. Restore player position/inventory if spawned
        plugin.getSpawnManager().handleLoginRestore(player);

        // 6. Send to Proxy Lobby if enabled
        plugin.getProxyBridge().sendToLobby(player);

        // 7. Show Welcome Title
        Title welcomeTitle = Title.title(
                plugin.getLocaleManager().parse("<green><bold>✔ ¡AUTENTICADO!</bold></green>"),
                plugin.getLocaleManager().parse("<gold>Bienvenido, <yellow>" + player.getName() + "</yellow></gold>"),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))
        );
        player.showTitle(welcomeTitle);

        // 8. Send Chat Message with Prefix
        if (messageKey != null) {
            String prefix = plugin.getLocaleManager().getRawMessage("prefix", player);
            net.kyori.adventure.text.Component msg = plugin.getLocaleManager().getComponent(messageKey, player);
            if (prefix != null && !prefix.equals("prefix") && !prefix.isEmpty()) {
                player.sendMessage(plugin.getLocaleManager().parse(prefix).append(msg));
            } else {
                player.sendMessage(msg);
            }
        }
    }
}
