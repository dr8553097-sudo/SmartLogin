package com.dafealru.smartlogin.database;

import java.util.UUID;

public class PlayerProfile {

    private final UUID uuid;
    private String username;
    private String passwordHash;
    private String salt;
    private boolean is2FAEnabled;
    private String totpSecret;
    private String lastIp;
    private long lastLoginTimestamp;
    private boolean isPremium;
    private boolean isBedrock;

    public PlayerProfile(UUID uuid, String username, String passwordHash, String salt,
                         boolean is2FAEnabled, String totpSecret, String lastIp,
                         long lastLoginTimestamp, boolean isPremium, boolean isBedrock) {
        this.uuid = uuid;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.is2FAEnabled = is2FAEnabled;
        this.totpSecret = totpSecret;
        this.lastIp = lastIp;
        this.lastLoginTimestamp = lastLoginTimestamp;
        this.isPremium = isPremium;
        this.isBedrock = isBedrock;
    }

    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public boolean is2FAEnabled() { return is2FAEnabled; }
    public void set2FAEnabled(boolean is2FAEnabled) { this.is2FAEnabled = is2FAEnabled; }
    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }
    public long getLastLoginTimestamp() { return lastLoginTimestamp; }
    public void setLastLoginTimestamp(long lastLoginTimestamp) { this.lastLoginTimestamp = lastLoginTimestamp; }
    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }
    public boolean isBedrock() { return isBedrock; }
    public void setBedrock(boolean bedrock) { isBedrock = bedrock; }
}
