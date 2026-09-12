package com.dafealru.smartlogin;

import com.dafealru.smartlogin.auth.AuthManager;
import com.dafealru.smartlogin.auth.AutoLoginDetector;
import com.dafealru.smartlogin.auth.SessionShield;
import com.dafealru.smartlogin.commands.*;
import com.dafealru.smartlogin.config.ConfigManager;
import com.dafealru.smartlogin.config.LocaleManager;
import com.dafealru.smartlogin.database.DatabaseManager;
import com.dafealru.smartlogin.database.MySQLDatabase;
import com.dafealru.smartlogin.database.SQLiteDatabase;
import com.dafealru.smartlogin.gui.PinPadGUI;
import com.dafealru.smartlogin.listeners.PlayerConnectionListener;
import com.dafealru.smartlogin.listeners.PlayerSecurityListener;
import com.dafealru.smartlogin.qr.QrMapManager;
import com.dafealru.smartlogin.wizard.SetupWizardManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmartLogin extends JavaPlugin {

    private static SmartLogin instance;

    private ConfigManager configManager;
    private LocaleManager localeManager;
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private SessionShield sessionShield;
    private AutoLoginDetector autoLoginDetector;
    private QrMapManager qrMapManager;
    private SetupWizardManager setupWizardManager;
    private PinPadGUI pinPadGUI;

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        getLogger().info("==================================================");
        getLogger().info("⚡ SmartLogin v" + getPluginMeta().getVersion() + " — Initializing Suite");
        getLogger().info("Author: Dafealru | Native Java 21 & Paper 1.21.x");
        getLogger().info("==================================================");

        // 1. Configs & Locales
        this.configManager = new ConfigManager(this);
        this.configManager.load();
        this.localeManager = new LocaleManager(this);
        this.localeManager.load();

        // 2. Database
        if ("MYSQL".equalsIgnoreCase(configManager.getDbType())) {
            this.databaseManager = new MySQLDatabase(this);
        } else {
            this.databaseManager = new SQLiteDatabase(this);
        }
        this.databaseManager.initialize();

        // 3. Core Subsystems
        this.authManager = new AuthManager(this);
        this.sessionShield = new SessionShield(this);
        this.autoLoginDetector = new AutoLoginDetector(this);
        this.qrMapManager = new QrMapManager(this);
        this.setupWizardManager = new SetupWizardManager(this);
        this.pinPadGUI = new PinPadGUI(this);

        // 4. Register Listeners
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerSecurityListener(this), this);
        pm.registerEvents(new PlayerConnectionListener(this), this);

        // 5. Register Commands
        if (getCommand("login") != null) getCommand("login").setExecutor(new LoginCommand(this));
        if (getCommand("register") != null) getCommand("register").setExecutor(new RegisterCommand(this));
        if (getCommand("2fa") != null) getCommand("2fa").setExecutor(new TwoFactorCommand(this));
        if (getCommand("changepassword") != null) getCommand("changepassword").setExecutor(new ChangePasswordCommand(this));
        if (getCommand("premium") != null) getCommand("premium").setExecutor(new PremiumCommand(this));
        if (getCommand("cracked") != null) getCommand("cracked").setExecutor(new PremiumCommand(this));
        if (getCommand("smartlogin") != null) getCommand("smartlogin").setExecutor(new SmartLoginAdminCommand(this));

        long elapsed = System.currentTimeMillis() - startTime;
        getLogger().info("✔ SmartLogin fully enabled in " + elapsed + "ms! System ready.");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("SmartLogin successfully disabled. Bye!");
    }

    public static SmartLogin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public LocaleManager getLocaleManager() { return localeManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public AuthManager getAuthManager() { return authManager; }
    public SessionShield getSessionShield() { return sessionShield; }
    public AutoLoginDetector getAutoLoginDetector() { return autoLoginDetector; }
    public QrMapManager getQrMapManager() { return qrMapManager; }
    public SetupWizardManager getSetupWizardManager() { return setupWizardManager; }
    public PinPadGUI getPinPadGUI() { return pinPadGUI; }
}
