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
                sender.sendMessage("§cAuthMe SQLite database not found at plugins/AuthMe/authme.db");
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

                    UUID fakeUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username.toLowerCase()).getBytes());
                    PlayerProfile profile = new PlayerProfile(fakeUuid, username, hash, "", false, null, null, ip, lastLogin, false, false);
                    plugin.getDatabaseManager().saveProfile(profile).join();
                    count++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return count;
        });
    }

    public CompletableFuture<Integer> importNLogin(CommandSender sender) {
        return CompletableFuture.supplyAsync(() -> {
            File nloginDb = new File(plugin.getDataFolder().getParentFile(), "nLogin/database.db");
            if (!nloginDb.exists()) {
                sender.sendMessage("§cnLogin SQLite database not found at plugins/nLogin/database.db");
                return 0;
            }

            int count = 0;
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + nloginDb.getAbsolutePath());
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM accounts")) {

                while (rs.next()) {
                    String username = rs.getString("name");
                    String hash = rs.getString("password");
                    String ip = rs.getString("last_ip");
                    long lastLogin = rs.getLong("last_seen");

                    UUID fakeUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username.toLowerCase()).getBytes());
                    PlayerProfile profile = new PlayerProfile(fakeUuid, username, hash, "", false, null, null, ip, lastLogin, false, false);
                    plugin.getDatabaseManager().saveProfile(profile).join();
                    count++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return count;
        });
    }
}
