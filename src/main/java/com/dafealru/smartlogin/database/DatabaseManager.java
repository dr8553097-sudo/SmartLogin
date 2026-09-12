package com.dafealru.smartlogin.database;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DatabaseManager {
    void initialize();
    void close();
    CompletableFuture<PlayerProfile> loadProfile(UUID uuid);
    CompletableFuture<PlayerProfile> loadProfileByName(String username);
    CompletableFuture<Void> saveProfile(PlayerProfile profile);
    CompletableFuture<Void> deleteProfile(UUID uuid);
    CompletableFuture<Integer> countAccountsByIp(String ip);
}
