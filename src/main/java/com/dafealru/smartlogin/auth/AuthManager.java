package com.dafealru.smartlogin.auth;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.entity.Player;
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

    public AuthState getState(UUID uuid) {
        return playerStates.getOrDefault(uuid, AuthState.UNAUTHENTICATED);
    }

    public void setState(UUID uuid, AuthState state) {
        playerStates.put(uuid, state);
    }

    public void cacheProfile(PlayerProfile profile) {
        if (profile != null) {
            cachedProfiles.put(profile.getUuid(), profile);
        }
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
}
