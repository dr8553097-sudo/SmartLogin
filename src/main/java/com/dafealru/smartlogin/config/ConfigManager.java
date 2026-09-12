package com.dafealru.smartlogin.config;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

public class ConfigManager {

    private final SmartLogin plugin;
    private String language;
    private String dbType;
    private String mysqlHost, mysqlDatabase, mysqlUser, mysqlPassword;
    private int mysqlPort, mysqlPoolSize;
    private boolean mysqlSsl;
    private int sessionTimeoutMinutes, maxAccountsPerIp, minPasswordLength, timeoutSeconds;
    private List<String> blockedPasswords;
    private boolean bedrockAutoLogin, premiumAutoLogin, totpEnabled, blindnessEnabled;
    private String issuerName;
    private List<String> allowedCommands;

    public ConfigManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        language = cfg.getString("language", "en");
        dbType = cfg.getString("database.type", "SQLITE").toUpperCase();

        mysqlHost = cfg.getString("database.mysql.host", "localhost");
        mysqlPort = cfg.getInt("database.mysql.port", 3306);
        mysqlDatabase = cfg.getString("database.mysql.database", "smartlogin");
        mysqlUser = cfg.getString("database.mysql.username", "root");
        mysqlPassword = cfg.getString("database.mysql.password", "");
        mysqlPoolSize = cfg.getInt("database.mysql.pool_size", 10);
        mysqlSsl = cfg.getBoolean("database.mysql.use_ssl", false);

        sessionTimeoutMinutes = cfg.getInt("auth.session_timeout_minutes", 180);
        maxAccountsPerIp = cfg.getInt("auth.max_accounts_per_ip", 3);
        minPasswordLength = cfg.getInt("auth.min_password_length", 5);
        timeoutSeconds = cfg.getInt("auth.timeout_seconds", 45);
        blockedPasswords = cfg.getStringList("auth.blocked_passwords");

        bedrockAutoLogin = cfg.getBoolean("tri_mode.bedrock_auto_login", true);
        premiumAutoLogin = cfg.getBoolean("tri_mode.premium_auto_login", true);

        totpEnabled = cfg.getBoolean("two_factor.enable_totp", true);
        issuerName = cfg.getString("two_factor.issuer_name", "AurexCraft");

        blindnessEnabled = cfg.getBoolean("lockdown.apply_blindness", true);
        allowedCommands = cfg.getStringList("lockdown.allowed_commands");
    }

    public void toggleOption(String option) {
        FileConfiguration cfg = plugin.getConfig();
        switch (option.toLowerCase()) {
            case "bedrock" -> {
                bedrockAutoLogin = !bedrockAutoLogin;
                cfg.set("tri_mode.bedrock_auto_login", bedrockAutoLogin);
            }
            case "premium" -> {
                premiumAutoLogin = !premiumAutoLogin;
                cfg.set("tri_mode.premium_auto_login", premiumAutoLogin);
            }
            case "totp" -> {
                totpEnabled = !totpEnabled;
                cfg.set("two_factor.enable_totp", totpEnabled);
            }
            case "blindness" -> {
                blindnessEnabled = !blindnessEnabled;
                cfg.set("lockdown.apply_blindness", blindnessEnabled);
            }
        }
        plugin.saveConfig();
    }

    public String getLanguage() { return language; }
    public String getDbType() { return dbType; }
    public String getMysqlHost() { return mysqlHost; }
    public int getMysqlPort() { return mysqlPort; }
    public String getMysqlDatabase() { return mysqlDatabase; }
    public String getMysqlUser() { return mysqlUser; }
    public String getMysqlPassword() { return mysqlPassword; }
    public int getMysqlPoolSize() { return mysqlPoolSize; }
    public boolean isMysqlSsl() { return mysqlSsl; }
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public int getMaxAccountsPerIp() { return maxAccountsPerIp; }
    public int getMinPasswordLength() { return minPasswordLength; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public List<String> getBlockedPasswords() { return blockedPasswords; }
    public boolean isBedrockAutoLogin() { return bedrockAutoLogin; }
    public boolean isPremiumAutoLogin() { return premiumAutoLogin; }
    public boolean isTotpEnabled() { return totpEnabled; }
    public String getIssuerName() { return issuerName; }
    public boolean isBlindnessEnabled() { return blindnessEnabled; }
    public List<String> getAllowedCommands() { return allowedCommands; }
}
