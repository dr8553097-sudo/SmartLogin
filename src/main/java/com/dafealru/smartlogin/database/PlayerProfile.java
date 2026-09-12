package com.dafealru.smartlogin.database;

import java.util.UUID;

public class PlayerProfile {

    private final UUID uuid;
    private String username;
    private String passwordHash;
    private String salt;
    private boolean twoFactorEnabled;
    private String totpSecret;
    private String backupCodes;
    private String discordId;
    private String telegramChatId;
    private String email;
    private String lastIp;
    private long lastLoginTimestamp;
    private boolean isPremium;
    private boolean isBedrock;

    public PlayerProfile(UUID uuid, String username, String passwordHash, String salt,
                         boolean twoFactorEnabled, String totpSecret, String backupCodes,
                         String discordId, String telegramChatId, String email,
                         String lastIp, long lastLoginTimestamp, boolean isPremium, boolean isBedrock) {
        this.uuid = uuid;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.twoFactorEnabled = twoFactorEnabled;
        this.totpSecret = totpSecret;
        this.backupCodes = backupCodes;
        this.discordId = discordId;
        this.telegramChatId = telegramChatId;
        this.email = email;
        this.lastIp = lastIp;
        this.lastLoginTimestamp = lastLoginTimestamp;
        this.isPremium = isPremium;
        this.isBedrock = isBedrock;
    }

    public PlayerProfile(UUID uuid, String username, String passwordHash, String salt,
                         boolean twoFactorEnabled, String totpSecret, String backupCodes,
                         String lastIp, long lastLoginTimestamp, boolean isPremium, boolean isBedrock) {
        this(uuid, username, passwordHash, salt, twoFactorEnabled, totpSecret, backupCodes, null, null, null, lastIp, lastLoginTimestamp, isPremium, isBedrock);
    }

    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public boolean is2FAEnabled() { return twoFactorEnabled; }
    public void set2FAEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }
    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    public String getBackupCodes() { return backupCodes; }
    public void setBackupCodes(String backupCodes) { this.backupCodes = backupCodes; }
    public String getDiscordId() { return discordId; }
    public void setDiscordId(String discordId) { this.discordId = discordId; }
    public String getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }
    public long getLastLoginTimestamp() { return lastLoginTimestamp; }
    public void setLastLoginTimestamp(long lastLoginTimestamp) { this.lastLoginTimestamp = lastLoginTimestamp; }
    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }
    public boolean isBedrock() { return isBedrock; }
    public void setBedrock(boolean bedrock) { isBedrock = bedrock; }
}
