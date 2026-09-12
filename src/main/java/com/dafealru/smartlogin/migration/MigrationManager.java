package com.dafealru.smartlogin.migration;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MigrationManager {

    private final SmartLogin plugin;

    public MigrationManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Integer> importAuthMe(CommandSender sender) {
        return CompletableFuture.supplyAsync(() -> {
            File authMeDb = new File(plugin.getDataFolder().getParentFile(), "AuthMe/authme.db");
            if (!authMeDb.exists()) {
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>✖ No se encontró la base de datos de AuthMe en plugins/AuthMe/authme.db</red>"));
                return 0;
            }

            int count = 0;
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + authMeDb.getAbsolutePath());
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM authme")) {

                while (rs.next()) {
                    String username = rs.getString("username");
                    String hash = rs.getString("password");
                    String ip = rs.getString("ip");
                    long lastLogin = rs.getLong("lastlogin");
                    String email = null;
                    try { email = rs.getString("email"); } catch (Exception ignored) {}

                    UUID fakeUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username.toLowerCase()).getBytes());
                    PlayerProfile profile = new PlayerProfile(fakeUuid, username, hash, "", false, null, null, null, null, email, ip, lastLogin, false, false);
                    plugin.getDatabaseManager().saveProfile(profile).join();
                    count++;
                }
            } catch (Exception e) {
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>✖ Error migrando AuthMe: " + e.getMessage() + "</red>"));
            }
            return count;
        });
    }

    public CompletableFuture<Integer> importNLogin(CommandSender sender) {
        return CompletableFuture.supplyAsync(() -> {
            File nloginDb = new File(plugin.getDataFolder().getParentFile(), "nLogin/database.db");
            if (!nloginDb.exists()) {
                nloginDb = new File(plugin.getDataFolder().getParentFile(), "nLogin/accounts.db");
            }
            if (!nloginDb.exists()) {
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>✖ No se encontró la base de datos de nLogin en plugins/nLogin/</red>"));
                return 0;
            }

            int count = 0;
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + nloginDb.getAbsolutePath());
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM accounts")) {

                while (rs.next()) {
                    String username = rs.getString("name");
                    String hash = rs.getString("password");
                    String ip = "";
                    try { ip = rs.getString("last_ip"); } catch (Exception ignored) {}
                    long lastLogin = 0;
                    try { lastLogin = rs.getLong("last_seen"); } catch (Exception ignored) {}

                    UUID fakeUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username.toLowerCase()).getBytes());
                    PlayerProfile profile = new PlayerProfile(fakeUuid, username, hash, "", false, null, null, ip, lastLogin, false, false);
                    plugin.getDatabaseManager().saveProfile(profile).join();
                    count++;
                }
            } catch (Exception e) {
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>✖ Error migrando nLogin: " + e.getMessage() + "</red>"));
            }
            return count;
        });
    }

    public CompletableFuture<Integer> importFastLogin(CommandSender sender) {
        return CompletableFuture.supplyAsync(() -> {
            File fastLoginDb = new File(plugin.getDataFolder().getParentFile(), "FastLogin/fastlogin.db");
            if (!fastLoginDb.exists()) {
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>✖ No se encontró la base de datos de FastLogin en plugins/FastLogin/fastlogin.db</red>"));
                return 0;
            }

            int count = 0;
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + fastLoginDb.getAbsolutePath());
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM premium")) {

                while (rs.next()) {
                    String username = rs.getString("player_name");
                    boolean isPremium = rs.getInt("premium") == 1;
                    plugin.getDatabaseManager().loadProfileByName(username).thenAccept(p -> {
                        if (p != null) {
                            p.setPremium(isPremium);
                            plugin.getDatabaseManager().saveProfile(p);
                        }
                    });
                    count++;
                }
            } catch (Exception e) {
                sender.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>✖ Error migrando FastLogin: " + e.getMessage() + "</red>"));
            }
            return count;
        });
    }
}
