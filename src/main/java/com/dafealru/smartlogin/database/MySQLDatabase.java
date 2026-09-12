package com.dafealru.smartlogin.database;

import com.dafealru.smartlogin.SmartLogin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySQLDatabase implements DatabaseManager {

    private final SmartLogin plugin;
    private HikariDataSource dataSource;

    public MySQLDatabase(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            var cfg = plugin.getConfigManager();
            HikariConfig hcfg = new HikariConfig();
            hcfg.setJdbcUrl("jdbc:mysql://" + cfg.getMysqlHost() + ":" + cfg.getMysqlPort() + "/" + cfg.getMysqlDatabase() + "?useSSL=" + cfg.isMysqlSsl());
            hcfg.setUsername(cfg.getMysqlUser());
            hcfg.setPassword(cfg.getMysqlPassword());
            hcfg.setMaximumPoolSize(cfg.getMysqlPoolSize());
            hcfg.setPoolName("SmartLogin-HikariPool");

            dataSource = new HikariDataSource(hcfg);

            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS smart_users (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "username VARCHAR(32) NOT NULL, " +
                        "password_hash TEXT, " +
                        "salt TEXT, " +
                        "two_factor_enabled TINYINT(1) DEFAULT 0, " +
                        "totp_secret TEXT, " +
                        "last_ip VARCHAR(45), " +
                        "last_login BIGINT DEFAULT 0, " +
                        "is_premium TINYINT(1) DEFAULT 0, " +
                        "is_bedrock TINYINT(1) DEFAULT 0, " +
                        "INDEX idx_username (username), " +
                        "INDEX idx_ip (last_ip)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            }
            plugin.getLogger().info("MySQL Database (HikariCP) connected successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize MySQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    @Override
    public CompletableFuture<PlayerProfile> loadProfile(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM smart_users WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapProfile(rs);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<PlayerProfile> loadProfileByName(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM smart_users WHERE LOWER(username) = LOWER(?)")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapProfile(rs);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> saveProfile(PlayerProfile p) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO smart_users (uuid, username, password_hash, salt, two_factor_enabled, totp_secret, last_ip, last_login, is_premium, is_bedrock) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "username=VALUES(username), password_hash=VALUES(password_hash), salt=VALUES(salt), " +
                    "two_factor_enabled=VALUES(two_factor_enabled), totp_secret=VALUES(totp_secret), " +
                    "last_ip=VALUES(last_ip), last_login=VALUES(last_login), is_premium=VALUES(is_premium), is_bedrock=VALUES(is_bedrock)";
            try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getUuid().toString());
                ps.setString(2, p.getUsername());
                ps.setString(3, p.getPasswordHash());
                ps.setString(4, p.getSalt());
                ps.setInt(5, p.is2FAEnabled() ? 1 : 0);
                ps.setString(6, p.getTotpSecret());
                ps.setString(7, p.getLastIp());
                ps.setLong(8, p.getLastLoginTimestamp());
                ps.setInt(9, p.isPremium() ? 1 : 0);
                ps.setInt(10, p.isBedrock() ? 1 : 0);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteProfile(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM smart_users WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Integer> countAccountsByIp(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM smart_users WHERE last_ip = ?")) {
                ps.setString(1, ip);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                rs.getString("last_ip"),
                rs.getLong("last_login"),
                rs.getInt("is_premium") == 1,
                rs.getInt("is_bedrock") == 1
        );
    }
}
