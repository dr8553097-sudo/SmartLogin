package com.dafealru.smartlogin.config;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ModularConfigManager {

    private final SmartLogin plugin;

    private File configFile, authFile, databaseFile, twofactorFile, pinpadFile;
    private FileConfiguration config, authConfig, databaseConfig, twofactorConfig, pinpadConfig;

    public ModularConfigManager(SmartLogin plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public void loadAll() {
        configFile = setupFile("config.yml");
        authFile = setupFile("auth.yml");
        databaseFile = setupFile("database.yml");
        twofactorFile = setupFile("twofactor.yml");
        pinpadFile = setupFile("pinpad.yml");

        config = YamlConfiguration.loadConfiguration(configFile);
        authConfig = YamlConfiguration.loadConfiguration(authFile);
        databaseConfig = YamlConfiguration.loadConfiguration(databaseFile);
        twofactorConfig = YamlConfiguration.loadConfiguration(twofactorFile);
        pinpadConfig = YamlConfiguration.loadConfiguration(pinpadFile);
    }

    private File setupFile(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource(name, false);
        }
        return file;
    }

    public void saveConfig() {
        try { config.save(configFile); } catch (Exception e) { e.printStackTrace(); }
    }

    public void saveAuth() {
        try { authConfig.save(authFile); } catch (Exception e) { e.printStackTrace(); }
    }

    public void saveTwoFactor() {
        try { twofactorConfig.save(twofactorFile); } catch (Exception e) { e.printStackTrace(); }
    }

    public void savePinpad() {
        try { pinpadConfig.save(pinpadFile); } catch (Exception e) { e.printStackTrace(); }
    }

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getAuthConfig() { return authConfig; }
    public FileConfiguration getDatabaseConfig() { return databaseConfig; }
    public FileConfiguration getTwoFactorConfig() { return twofactorConfig; }
    public FileConfiguration getPinpadConfig() { return pinpadConfig; }
}
