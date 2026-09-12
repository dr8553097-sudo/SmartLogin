package com.dafealru.smartlogin.database;

import com.dafealru.smartlogin.SmartLogin;
import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLiteDatabase implements DatabaseManager {

    private final SmartLogin plugin;
    private Connection connection;
    private final File dbFile;

    public SQLiteDatabase(SmartLogin plugin) {
        this.plugin = plugin;
        String fileName = plugin.getModularConfig().getDatabaseConfig().getString("sqlite.file", "smartlogin.db");
        this.dbFile = new File(plugin.getDataFolder(), fileName);
    }

    @Override
    public void initialize() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            
            try (Statement st = connection.createStatement()) {
                if (plugin.getModularConfig().getDatabaseConfig().getBoolean("sqlite.wal-mode", true)) {
                    st.execute("PRAGMA journal_mode=WAL;");
                }
                st.execute("CREATE TABLE IF NOT EXISTS smart_users (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "username VARCHAR(32) NOT NULL, " +
                        "password_hash TEXT, " +
                        "salt TEXT, " +
                        "two_factor_enabled INTEGER DEFAULT 0, " +
                        "totp_secret TEXT, " +
                        "backup_codes TEXT, " +
                        "discord_id VARCHAR(32), " +
                        "email VARCHAR(128), " +
                        "last_ip VARCHAR(45), " +
                        "last_login INTEGER DEFAULT 0, " +
                        "is_premium INTEGER DEFAULT 0, " +
                        "is_bedrock INTEGER DEFAULT 0" +
                        ");");
                st.execute("CREATE INDEX IF NOT EXISTS idx_user_name ON smart_users(username);");
                st.execute("CREATE INDEX IF NOT EXISTS idx_user_ip ON smart_users(last_ip);");
            }
            plugin.getLogger().info("SQLite Database initialized successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize SQLite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }
        return connection;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }

    @Override
    public CompletableFuture<PlayerProfile> loadProfile(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM smart_users WHERE uuid = ?";
                try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return mapProfile(rs);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
            return null;
        });
    }

    @Override
    public CompletableFuture<PlayerProfile> loadProfileByName(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM smart_users WHERE LOWER(username) = LOWER(?)";
                try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return mapProfile(rs);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> saveProfile(PlayerProfile p) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql = "INSERT INTO smart_users (uuid, username, password_hash, salt, two_factor_enabled, totp_secret, backup_codes, discord_id, email, last_ip, last_login, is_premium, is_bedrock) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET " +
                        "username=excluded.username, password_hash=excluded.password_hash, salt=excluded.salt, " +
                        "two_factor_enabled=excluded.two_factor_enabled, totp_secret=excluded.totp_secret, backup_codes=excluded.backup_codes, " +
                        "discord_id=excluded.discord_id, email=excluded.email, " +
                        "last_ip=excluded.last_ip, last_login=excluded.last_login, is_premium=excluded.is_premium, is_bedrock=excluded.is_bedrock";
                try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                    ps.setString(1, p.getUuid().toString());
                    ps.setString(2, p.getUsername());
                    ps.setString(3, p.getPasswordHash());
                    ps.setString(4, p.getSalt());
                    ps.setInt(5, p.is2FAEnabled() ? 1 : 0);
                    ps.setString(6, p.getTotpSecret());
                    ps.setString(7, p.getBackupCodes());
                    ps.setString(8, p.getDiscordId());
                    ps.setString(9, p.getEmail());
                    ps.setString(10, p.getLastIp());
                    ps.setLong(11, p.getLastLoginTimestamp());
                    ps.setInt(12, p.isPremium() ? 1 : 0);
                    ps.setInt(13, p.isBedrock() ? 1 : 0);
                    ps.executeUpdate();
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProfile(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql = "DELETE FROM smart_users WHERE uuid = ?";
                try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @Override
    public CompletableFuture<Integer> countAccountsByIp(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT COUNT(*) FROM smart_users WHERE last_ip = ?";
                try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                    ps.setString(1, ip);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return rs.getInt(1);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
            return 0;
        });
    }

    private PlayerProfile mapProfile(ResultSet rs) throws SQLException {
        return new PlayerProfile(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("salt"),
                rs.getInt("two_factor_enabled") == 1,
                rs.getString("totp_secret"),
                rs.getString("backup_codes"),
                rs.getString("discord_id"),
                rs.getString("email"),
                rs.getString("last_ip"),
                rs.getLong("last_login"),
                rs.getInt("is_premium") == 1,
                rs.getInt("is_bedrock") == 1
        );
    }
}
