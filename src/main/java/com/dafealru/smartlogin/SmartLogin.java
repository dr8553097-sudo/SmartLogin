package com.dafealru.smartlogin;

import com.dafealru.smartlogin.audit.AuditManager;
import com.dafealru.smartlogin.audit.DiagnosticManager;
import com.dafealru.smartlogin.gui.SecurityCenterGUI;
import com.dafealru.smartlogin.auth.AuthManager;
import com.dafealru.smartlogin.auth.AutoLoginDetector;
import com.dafealru.smartlogin.auth.SessionShield;
import com.dafealru.smartlogin.captcha.CaptchaManager;
import com.dafealru.smartlogin.commands.*;
import com.dafealru.smartlogin.config.LocaleManager;
import com.dafealru.smartlogin.config.ModularConfigManager;
import com.dafealru.smartlogin.database.DatabaseManager;
import com.dafealru.smartlogin.database.MySQLDatabase;
import com.dafealru.smartlogin.database.SQLiteDatabase;
import com.dafealru.smartlogin.discord.DiscordManager;
import com.dafealru.smartlogin.geoip.GeoIpManager;
import com.dafealru.smartlogin.gui.AdminPanelGUI;
import com.dafealru.smartlogin.gui.PinPadGUI;
import com.dafealru.smartlogin.hud.AuthHudManager;
import com.dafealru.smartlogin.listeners.PlayerConnectionListener;
import com.dafealru.smartlogin.listeners.PlayerSecurityListener;
import com.dafealru.smartlogin.migration.MigrationManager;
import com.dafealru.smartlogin.proxy.ProxyBridge;
import com.dafealru.smartlogin.qr.QrMapManager;
import com.dafealru.smartlogin.spawn.SpawnManager;
import com.dafealru.smartlogin.telegram.TelegramManager;
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
    private TelegramManager telegramManager;
    private CaptchaManager captchaManager;
    private GeoIpManager geoIpManager;
    private AdminPanelGUI adminPanelGUI;
    private PinPadGUI pinPadGUI;
    private AuditManager auditManager;
    private ProxyBridge proxyBridge;
    private com.dafealru.smartlogin.audit.DiagnosticManager diagnosticManager;
    private com.dafealru.smartlogin.gui.SecurityCenterGUI securityCenterGUI;
    private com.dafealru.smartlogin.inventory.GhostInventoryManager ghostInventoryManager;
    private com.dafealru.smartlogin.bedrock.BedrockFormManager bedrockFormManager;
    private com.dafealru.smartlogin.recovery.AccountRecoveryManager accountRecoveryManager;
    private com.dafealru.smartlogin.streamer.StreamerManager streamerManager;
    private com.dafealru.smartlogin.email.EmailManager emailManager;
    private com.dafealru.smartlogin.antibot.AntiBotEngine antiBotEngine;

    @Override
    public void onEnable() {
        long startMs = System.currentTimeMillis();
        instance = this;

        var console = getServer().getConsoleSender();
        var mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

        console.sendMessage(mm.deserialize("<gradient:#7E22CE:#C084FC>================================================================</gradient>"));
        console.sendMessage(mm.deserialize("<gradient:#9333EA:#C084FC><bold>  ____                       _   _                 _       </bold></gradient>"));
        console.sendMessage(mm.deserialize("<gradient:#9333EA:#C084FC><bold> / ___| _ __ ___   __ _ _ __| |_| |     ___   __ _(_)_ __  </bold></gradient>"));
        console.sendMessage(mm.deserialize("<gradient:#9333EA:#C084FC><bold> \\___ \\| '_ ` _ \\ / _` | '__| __| |    / _ \\ / _` | | '_ \\ </bold></gradient>"));
        console.sendMessage(mm.deserialize("<gradient:#9333EA:#C084FC><bold>  ___) | | | | | | (_| | |  | |_| |___| (_) | (_| | | | | |</bold></gradient>"));
        console.sendMessage(mm.deserialize("<gradient:#9333EA:#C084FC><bold> |____/|_| |_| |_|\\__,_|_|   \\__|_____|\\___/ \\__, |_|_| |_|</bold></gradient>"));
        console.sendMessage(mm.deserialize("<gradient:#9333EA:#C084FC><bold>                                             |___/         </bold></gradient>"));
        console.sendMessage(mm.deserialize(" <gradient:#C084FC:#F5D0FE><bold>⚡ SmartLogin v" + getPluginMeta().getVersion() + "</bold></gradient> <dark_gray>—</dark_gray> <color:#E9D5FF>Advanced Next-Gen Auth Engine</color>"));
        console.sendMessage(mm.deserialize(" <gradient:#C084FC:#F5D0FE>👑 <bold>Creador / Autor:</bold></gradient> <color:#F5D0FE><bold>Dafealru</bold></color>"));
        console.sendMessage(mm.deserialize(" <gradient:#C084FC:#F5D0FE>🌐 <bold>Plataforma:</bold></gradient> <color:#E9D5FF>Native Java 21 & Paper/Purpur 1.21.x</color>"));
        console.sendMessage(mm.deserialize("<gradient:#7E22CE:#C084FC>================================================================</gradient>"));

        // 1. Modular Configs & Locales
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
        this.telegramManager = new TelegramManager(this);
        this.captchaManager = new CaptchaManager(this);
        this.geoIpManager = new GeoIpManager(this);
        this.adminPanelGUI = new AdminPanelGUI(this);
        this.auditManager = new AuditManager(this);
        this.proxyBridge = new ProxyBridge(this);
        this.diagnosticManager = new DiagnosticManager(this);
        this.securityCenterGUI = new SecurityCenterGUI(this);
        this.ghostInventoryManager = new com.dafealru.smartlogin.inventory.GhostInventoryManager(this);
        this.bedrockFormManager = new com.dafealru.smartlogin.bedrock.BedrockFormManager(this);
        this.accountRecoveryManager = new com.dafealru.smartlogin.recovery.AccountRecoveryManager(this);
        this.streamerManager = new com.dafealru.smartlogin.streamer.StreamerManager(this);
        this.emailManager = new com.dafealru.smartlogin.email.EmailManager(this);
        this.antiBotEngine = new com.dafealru.smartlogin.antibot.AntiBotEngine(this);

        // Initialize GUI components
        this.pinPadGUI = new PinPadGUI(this);
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerSecurityListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(this.antiBotEngine, this);
        getServer().getPluginManager().registerEvents(this.pinPadGUI, this);
        getServer().getPluginManager().registerEvents(this.captchaManager, this);
        getServer().getPluginManager().registerEvents(this.adminPanelGUI, this);
        getServer().getPluginManager().registerEvents(this.securityCenterGUI, this);

        // 5. Commands
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("changepassword").setExecutor(new ChangePasswordCommand(this));
        getCommand("2fa").setExecutor(new TwoFactorCommand(this));
        getCommand("premium").setExecutor(new PremiumCommand(this));
        
        SmartLoginAdminCommand adminCmd = new SmartLoginAdminCommand(this);
        getCommand("smartlogin").setExecutor(adminCmd);
        getCommand("smartlogin").setTabCompleter(adminCmd);

        if (getCommand("link") != null) {
            LinkCommand linkCmd = new LinkCommand(this);
            getCommand("link").setExecutor(linkCmd);
            getCommand("link").setTabCompleter(linkCmd);
        }
        if (getCommand("unlink") != null) {
            UnlinkCommand unlinkCmd = new UnlinkCommand(this);
            getCommand("unlink").setExecutor(unlinkCmd);
            getCommand("unlink").setTabCompleter(unlinkCmd);
        }
        if (getCommand("tlink") != null) {
            getCommand("tlink").setExecutor(new TelegramLinkCommand(this));
        }
        if (getCommand("lang") != null) {
            LanguageCommand langCmd = new LanguageCommand(this);
            getCommand("lang").setExecutor(langCmd);
            getCommand("lang").setTabCompleter(langCmd);
        }
        if (getCommand("security") != null) {
            getCommand("security").setExecutor(new SecurityCommand(this));
        }
        if (getCommand("recover") != null) {
            getCommand("recover").setExecutor(new RecoverCommand(this));
        }
        if (getCommand("streamer") != null) {
            getCommand("streamer").setExecutor(new StreamerCommand(this));
        }
        if (getCommand("email") != null) {
            getCommand("email").setExecutor(new EmailCommand(this));
        }

        long elapsed = System.currentTimeMillis() - startMs;
        console.sendMessage(mm.deserialize("<gradient:#9333EA:#C084FC>✔ <bold>SmartLogin</bold> fully enabled in <color:#F5D0FE>" + elapsed + "ms</color>! Sistema listo.</gradient>"));
    }

    @Override
    public void onDisable() {
        if (this.authHudManager != null) {
            this.authHudManager.shutdown();
        }
        if (this.ghostInventoryManager != null) {
            this.ghostInventoryManager.restoreAll();
        }
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
        getServer().getScheduler().cancelTasks(this);
    }

    // Refresh UI elements for a player after language change
    public void refreshPlayerUI(org.bukkit.entity.Player player) {
        // Refresh HUD if player is not authenticated
        if (!this.authManager.isAuthenticated(player.getUniqueId())) {
            // Stop any existing HUD and restart with correct language
            this.authHudManager.stopHud(player);
            this.databaseManager.loadProfile(player.getUniqueId()).thenAccept(profile -> {
                boolean isRegister = profile == null || profile.getPasswordHash() == null;
                org.bukkit.Bukkit.getScheduler().runTask(this, () -> {
                    this.authHudManager.startHud(player, isRegister);
                });
            });
        }
        // Refresh PinPad GUI if open
        if (player.getOpenInventory() != null && player.getOpenInventory().getTitle() != null) {
            String title = player.getOpenInventory().getTitle().toString();
            if (title.contains("🔒")) { // PinPad title contains lock emoji
                if (this.pinPadGUI != null) {
                    this.pinPadGUI.openPinPad(player);
                }
            }
        }
        // Refresh Admin Panel GUI if open (more robust detection)
        if (player.getOpenInventory() != null && player.getOpenInventory().getTitle() != null) {
            String title = player.getOpenInventory().getTitle().toString();
            // Detect any admin panel title that includes the plugin name or the lock emoji
            if (title.contains("SmartLogin") || title.contains("⚡")) {
                if (this.adminPanelGUI != null) {
                    this.adminPanelGUI.openPanel(player);
                }
            }
        }
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
    public TelegramManager getTelegramManager() { return telegramManager; }
    public CaptchaManager getCaptchaManager() { return captchaManager; }
    public GeoIpManager getGeoIpManager() { return geoIpManager; }
    public AdminPanelGUI getAdminPanelGUI() { return adminPanelGUI; }
    public AuditManager getAuditManager() { return auditManager; }
    public ProxyBridge getProxyBridge() { return proxyBridge; }
    public com.dafealru.smartlogin.audit.DiagnosticManager getDiagnosticManager() { return diagnosticManager; }
    public com.dafealru.smartlogin.gui.SecurityCenterGUI getSecurityCenterGUI() { return securityCenterGUI; }
    public com.dafealru.smartlogin.inventory.GhostInventoryManager getGhostInventoryManager() { return ghostInventoryManager; }
    public com.dafealru.smartlogin.bedrock.BedrockFormManager getBedrockFormManager() { return bedrockFormManager; }
    public com.dafealru.smartlogin.recovery.AccountRecoveryManager getAccountRecoveryManager() { return accountRecoveryManager; }
    public com.dafealru.smartlogin.streamer.StreamerManager getStreamerManager() { return streamerManager; }
    public com.dafealru.smartlogin.email.EmailManager getEmailManager() { return emailManager; }
    public com.dafealru.smartlogin.antibot.AntiBotEngine getAntiBotEngine() { return antiBotEngine; }
}
