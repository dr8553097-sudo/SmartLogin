package com.dafealru.smartlogin;

import com.dafealru.smartlogin.audit.AuditManager;
import com.dafealru.smartlogin.auth.AuthManager;
import com.dafealru.smartlogin.auth.AutoLoginDetector;
import com.dafealru.smartlogin.auth.SessionShield;
import com.dafealru.smartlogin.commands.*;
import com.dafealru.smartlogin.config.LocaleManager;
import com.dafealru.smartlogin.config.ModularConfigManager;
import com.dafealru.smartlogin.database.DatabaseManager;
import com.dafealru.smartlogin.database.MySQLDatabase;
import com.dafealru.smartlogin.database.SQLiteDatabase;
import com.dafealru.smartlogin.discord.DiscordManager;
import com.dafealru.smartlogin.gui.PinPadGUI;
import com.dafealru.smartlogin.hud.AuthHudManager;
import com.dafealru.smartlogin.listeners.PlayerConnectionListener;
import com.dafealru.smartlogin.listeners.PlayerSecurityListener;
import com.dafealru.smartlogin.migration.MigrationManager;
import com.dafealru.smartlogin.proxy.ProxyBridge;
import com.dafealru.smartlogin.qr.QrMapManager;
import com.dafealru.smartlogin.spawn.SpawnManager;
import com.dafealru.smartlogin.twofactor.TwoFactorManager;
import com.dafealru.smartlogin.wizard.SetupWizardManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmartLogin extends JavaPlugin {

    private static SmartLogin instance;

    private ModularConfigManager modularConfig;
    private LocaleManager localeManager;
    private DatabaseManager databaseManager;
    private AuthManager authManager;
    private SessionShield sessionShield;
    private AutoLoginDetector autoLoginDetector;
    private QrMapManager qrMapManager;
    private SetupWizardManager setupWizardManager;
    private TwoFactorManager twoFactorManager;
    private SpawnManager spawnManager;
    private MigrationManager migrationManager;
    private AuthHudManager authHudManager;
    private DiscordManager discordManager;
    private AuditManager auditManager;
    private ProxyBridge proxyBridge;

    @Override
    public void onEnable() {
        long startMs = System.currentTimeMillis();
        instance = this;

        getLogger().info("==================================================");
        getLogger().info("⚡ SmartLogin v" + getPluginMeta().getVersion() + " — Initializing Suite");
        getLogger().info("Author: Dafealru | Native Java 21 & Paper 1.21.x");
        getLogger().info("==================================================");

        // 1. Modular Configs (including 2fa/ folder) & Locales
        this.modularConfig = new ModularConfigManager(this);
        this.localeManager = new LocaleManager(this);

        // 2. Database Engine
        String dbType = modularConfig.getDatabaseConfig().getString("type", "SQLITE").toUpperCase();
        if ("MYSQL".equalsIgnoreCase(dbType)) {
            this.databaseManager = new MySQLDatabase(this);
        } else {
            this.databaseManager = new SQLiteDatabase(this);
        }
        this.databaseManager.initialize();

        // 3. Subsystems
        this.authManager = new AuthManager(this);
        this.sessionShield = new SessionShield(this);
        this.autoLoginDetector = new AutoLoginDetector(this);
        this.qrMapManager = new QrMapManager(this);
        this.setupWizardManager = new SetupWizardManager(this);
        this.twoFactorManager = new TwoFactorManager(this);
        this.spawnManager = new SpawnManager(this);
        this.migrationManager = new MigrationManager(this);
        this.authHudManager = new AuthHudManager(this);
        this.discordManager = new DiscordManager(this);
        this.auditManager = new AuditManager(this);
        this.proxyBridge = new ProxyBridge(this);

        // 4. Listeners
        getServer().getPluginManager().registerEvents(new PlayerSecurityListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PinPadGUI(this), this);

        // 5. Commands
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("changepassword").setExecutor(new ChangePasswordCommand(this));
        getCommand("2fa").setExecutor(new TwoFactorCommand(this));
        getCommand("premium").setExecutor(new PremiumCommand(this));
        getCommand("smartlogin").setExecutor(new SmartLoginAdminCommand(this));
        if (getCommand("link") != null) {
            getCommand("link").setExecutor(new LinkCommand(this));
        }

        long elapsed = System.currentTimeMillis() - startMs;
        getLogger().info("✔ SmartLogin fully enabled in " + elapsed + "ms! System ready.");
    }

    @Override
    public void onDisable() {
        if (this.authHudManager != null) {
            this.authHudManager.shutdown();
        }
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
        getLogger().info("SmartLogin successfully disabled. Bye!");
    }

    public static SmartLogin getInstance() { return instance; }
    public ModularConfigManager getModularConfig() { return modularConfig; }
    public LocaleManager getLocaleManager() { return localeManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public AuthManager getAuthManager() { return authManager; }
    public SessionShield getSessionShield() { return sessionShield; }
    public AutoLoginDetector getAutoLoginDetector() { return autoLoginDetector; }
    public QrMapManager getQrMapManager() { return qrMapManager; }
    public SetupWizardManager getSetupWizardManager() { return setupWizardManager; }
    public TwoFactorManager getTwoFactorManager() { return twoFactorManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public MigrationManager getMigrationManager() { return migrationManager; }
    public AuthHudManager getAuthHudManager() { return authHudManager; }
    public DiscordManager getDiscordManager() { return discordManager; }
    public AuditManager getAuditManager() { return auditManager; }
    public ProxyBridge getProxyBridge() { return proxyBridge; }
}
