package com.dafealru.smartlogin.config;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ModularConfigManager {

    private final SmartLogin plugin;

    private File configFile, authFile, databaseFile;
    private File totpFile, discordFile, pinpadFile, emailFile;

    private FileConfiguration config, authConfig, databaseConfig;
    private FileConfiguration totpConfig, discordConfig, pinpadConfig, emailConfig;

    public ModularConfigManager(SmartLogin plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public void loadAll() {
        configFile = setupFile("config.yml");
        authFile = setupFile("auth.yml");
        databaseFile = setupFile("database.yml");

        File twoFaDir = new File(plugin.getDataFolder(), "2fa");
        if (!twoFaDir.exists()) twoFaDir.mkdirs();

        totpFile = setupSubFile("2fa/totp.yml");
        discordFile = setupSubFile("2fa/discord.yml");
        pinpadFile = setupSubFile("2fa/pinpad.yml");
        emailFile = setupSubFile("2fa/email.yml");

        config = YamlConfiguration.loadConfiguration(configFile);
        authConfig = YamlConfiguration.loadConfiguration(authFile);
        databaseConfig = YamlConfiguration.loadConfiguration(databaseFile);

        totpConfig = YamlConfiguration.loadConfiguration(totpFile);
        discordConfig = YamlConfiguration.loadConfiguration(discordFile);
        pinpadConfig = YamlConfiguration.loadConfiguration(pinpadFile);
        emailConfig = YamlConfiguration.loadConfiguration(emailFile);
    }

    private File setupFile(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource(name, false);
        }
        return file;
    }

    private File setupSubFile(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        return file;
    }

    public void saveConfig() { try { config.save(configFile); } catch (Exception e) { e.printStackTrace(); } }
    public void saveAuth() { try { authConfig.save(authFile); } catch (Exception e) { e.printStackTrace(); } }
    public void saveTotp() { try { totpConfig.save(totpFile); } catch (Exception e) { e.printStackTrace(); } }
    public void saveDiscord() { try { discordConfig.save(discordFile); } catch (Exception e) { e.printStackTrace(); } }
    public void savePinpad() { try { pinpadConfig.save(pinpadFile); } catch (Exception e) { e.printStackTrace(); } }
    public void saveEmail() { try { emailConfig.save(emailFile); } catch (Exception e) { e.printStackTrace(); } }

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getAuthConfig() { return authConfig; }
    public FileConfiguration getDatabaseConfig() { return databaseConfig; }
    public FileConfiguration getTotpConfig() { return totpConfig; }
    public FileConfiguration getTwoFactorConfig() { return totpConfig; }
    public FileConfiguration getDiscordConfig() { return discordConfig; }
    public FileConfiguration getPinpadConfig() { return pinpadConfig; }
    public FileConfiguration getEmailConfig() { return emailConfig; }
}
